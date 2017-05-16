(ns de.otto.tesla.system-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as c]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]
            [ring.mock.request :as mock]
            [de.otto.tesla.stateful.handler :as handler]
            [de.otto.tesla.stateful.configuring :as configuring]
            [environ.core :as env]
            [metrics.counters :as counter]
            [metrics.histograms :as hists]
            [metrics.gauges :as gauges]
            [de.otto.tesla.util.test-utils :refer [eventually]]
            [overtone.at-at :as at]
            [de.otto.tesla.stateful.scheduler :as scheduler]
            [metrics.core :as metrics]
            [de.otto.tesla.stateful.metering :as metering]
            [clojure.tools.logging :as log]))

(deftest ^:unit should-start-base-system-and-shut-it-down
  (testing "start then shutdown using own method"
    (let [started (system/start (system/base-system {}))
          _ (system/stop started)]
      (is (= "look ma, no exceptions" "look ma, no exceptions"))))

  (testing "start then shutdown using method from library"
    (let [started (system/start (system/base-system {}))
          _ (c/stop started)]
      (is (= "look ma, no exceptions" "look ma, no exceptions")))))

(deftest should-lock-application-on-shutdown
  (testing "the lock is set"
    (u/with-started
      [started (system/base-system {:wait-ms-on-stop 10})]
      (let [healthcomp (:health started)
            _ (system/stop started)]
        (is (= @(:locked healthcomp) true)))))

  (testing "it waits on stop"
    (u/with-started
      [started (system/base-system {:wait-seconds-on-stop 1})]
      (let [has-waited (atom false)]
        (with-redefs [system/wait! (fn [_] (reset! has-waited true))]
          (let [healthcomp (:health started)
                _ (system/stop started)]
            (is (= @(:locked healthcomp) true))
            (is (= @has-waited true))))))))


(deftest ^:integration should-substitute-env-variables-while-reading
  (with-redefs [env/env {:my-custom-status-url "/custom/status/path" :prop-without-fallback "some prop value"}]
    (u/with-started [started (system/base-system {})]
                    (testing "should load the status-path property from edn"
                      (is (= "/custom/status/path"
                             (:status-url (configuring/load-config-from-edn-files {})))))

                    (testing "should point to edn-configured custom status url"
                      (let [handlers (handler/handler (:handler started))
                            response (handlers (mock/request :get "/custom/status/path"))]
                        (is (= 200 (:status response)))))))

  (u/with-started [started (system/base-system {})]
                  (testing "should fallback to default for status path"
                    (is (= "/status"
                           (:status-url (configuring/load-config-from-edn-files {})))))))

(deftest the-scheduler-in-the-base-system
  (testing "should schedule and execute task NOW"
    (u/with-started [started (system/base-system {})]
                    (let [work-done (atom :no-work-done)
                          {:keys [scheduler]} started]
                      (at/after 0 #(reset! work-done :work-done!) (scheduler/pool scheduler))
                      (eventually (= :work-done! @work-done))))))

(def expected-metrics-endpoint-response
  {:body    "# TYPE default.default.test.gauge1 gauge
default.default.test.gauge1 42
# TYPE default.default.test.hist1 histogram
default.default.test.hist1{quantile=0.01} 5.0
default.default.test.hist1{quantile=0.05} 5.0
default.default.test.hist1{quantile=0.5} 7.0
default.default.test.hist1{quantile=0.9} 7.0
default.default.test.hist1{quantile=0.99} 7.0
default.default.test.hist1_sum 12
default.default.test.hist1_count 2
# TYPE default.default.test.counter1 counter
default.default.test.counter1 1
# TYPE default.default.test.counter2 counter
default.default.test.counter2 2
"
   :headers {"Content-Type" "application/json"}
   :status  200})

(deftest metrics-endpoint-test
  (testing "Should output all aggregated metrics in a prometheus readable representation"
    (u/with-started [started (system/base-system {})]
                    (metrics/remove-all-metrics)
                    (gauges/gauge-fn "test.gauge1" (constantly 42))
                    (counter/inc! (counter/counter "test.counter1"))
                    (counter/inc! (counter/counter "test.counter2") 2)
                    (hists/update! (hists/histogram "test.hist1") 5)
                    (hists/update! (hists/histogram "test.hist1") 7)
                    (is (= expected-metrics-endpoint-response
                           (metering/metrics-response (:metering started)))))))

(defrecord SingleRoute [single-route]
  c/Lifecycle
  (start [self]
    (handler/register-timed-handler (:handler self) single-route)
    self)
  (stop [self] self))

(defn ping-for [route-uri]
  (fn [{:keys [uri]}]
    (when (= uri route-uri)
      {:status 200
       :body   :ping})))

(deftest request-timing
  (testing "should time and report request for handler"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [timer-id _ time-taken] (swap! reportings conj [timer-id time-taken]))]
        (u/with-started [started (-> (system/base-system {})
                                     (dissoc :health :app-status :scheduler)
                                     (assoc
                                       :route0 (c/using (->SingleRoute (ping-for "/route-0")) [:handler])
                                       :route1 (c/using (->SingleRoute (ping-for "/route-1")) [:handler :route0])
                                       :route2 (c/using (->SingleRoute (ping-for "/route-2")) [:handler :route1])
                                       :route3 (c/using (->SingleRoute (ping-for "/route-3")) [:handler :route2])))]
                        (let [single-handler-fn (handler/handler (:handler started))]
                          (is (= 4 (count (filter :timer-path-fn @(:registered-handlers (:handler started))))))
                          (single-handler-fn (mock/request :get "/route-0"))
                          (single-handler-fn (mock/request :get "/route-1"))
                          (single-handler-fn (mock/request :get "/route-2"))
                          (single-handler-fn (mock/request :get "/route-3"))
                          (single-handler-fn (mock/request :get "/route-3"))
                          (single-handler-fn (mock/request :get "/route-3"))
                          (is (= [[["serving" "requests" "route-0" "200"] 100]
                                  [["serving" "requests" "route-1" "200"] 100]
                                  [["serving" "requests" "route-2" "200"] 100]
                                  [["serving" "requests" "route-3" "200"] 100]
                                  [["serving" "requests" "route-3" "200"] 100]
                                  [["serving" "requests" "route-3" "200"] 100]] @reportings))))))))
