(ns de.otto.tesla.stateful.scheduler-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.scheduler :as schedule]
            [de.otto.tesla.util.test-utils :as u]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.handler :as handler]
            [ring.mock.request :as mock]
            [clojure.data.json :as json]
            [de.otto.tesla.stateful.scheduler :as scheduler])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor)))

(defn- serverless-system [runtime-config]
  (-> (system/base-system runtime-config)
      (dissoc :server)
      (assoc :scheduler (c/using (schedule/new-scheduler) [:config :app-status]))))

(deftest ^:unit should-call-function-at-scheduled-rate
  (u/with-started [system (serverless-system {:host-name "bar" :server-port "0123"})]
                  (let [scheduler (:scheduler system)]
                    (testing "Function gets called every 10 ms"
                      (let [calls (atom 0)]
                        (at/every 20 #(swap! calls inc) (schedule/pool scheduler))
                        (Thread/sleep 50)
                        (is (= @calls 3))))

                    (testing "Function gets called every 10 ms with initial delay"
                      (let [calls (atom 0)]
                        (at/every 20 #(swap! calls inc) (schedule/pool scheduler) :initial-delay 20)
                        (Thread/sleep 50)
                        (is (= @calls 2))))

                    (testing "Function gets called every 10 ms AFTER the function last returned"
                      (let [calls (atom 0)]
                        (at/interspaced 20 #((Thread/sleep 10) (swap! calls inc)) (schedule/pool scheduler))
                        (Thread/sleep 50)
                        (is (= @calls 1)))))))

(defn assert-map-args! [args-assert-val]
  (fn [& {:as args}] (is (= args args-assert-val))))

(deftest ^:unit configuring-the-schedule
  (testing "should pass nr cpus to pool if specified"
    (let [config {:scheduler {:cpu-count      2
                              :stop-delayed?  false
                              :stop-periodic? true}}]
      (with-redefs [at/stop-and-reset-pool! (constantly nil)
                    at/mk-pool (assert-map-args! {:cpu-count      2
                                                  :stop-delayed?  false
                                                  :stop-periodic? true})]
        (u/with-started [system (serverless-system config)]
                        (is ())))))

  (testing "should pass nothing to pool if nothing is specified"
    (let [config {:some-other :property}]
      (with-redefs [at/stop-and-reset-pool! (constantly nil)
                    at/mk-pool (assert-map-args! {:cpu-count 0})]
        (u/with-started [system (serverless-system config)]
                        (is ()))))))

(deftest ^:unit scheduler-app-status
  (with-redefs [schedule/as-readable-time (constantly "mock-time")]
    (u/with-started [system (serverless-system {:host-name "bar" :server-port "0123"
                                                :scheduler {:cpu-count 2}})]
                    (let [{:keys [scheduler handler]} system
                          handler-fn (handler/handler handler)]
                      (testing "should register and return status-details in app-status"
                        (at/every 20 #(Thread/sleep 10) (schedule/pool scheduler) :desc "Job 1")
                        (at/interspaced 20 #(Thread/sleep 10) (schedule/pool scheduler) :desc "Job 2")
                        (Thread/sleep 10)
                        (is (= {:poolInfo      {:active    0
                                                :poolSize  2
                                                :queueSize 2}
                                :scheduledJobs {:1 {:createdAt    "mock-time"
                                                    :desc         "Job 1"
                                                    :initialDelay 0
                                                    :msPeriod     20
                                                    :scheduled?   true}
                                                :2 {:createdAt    "mock-time"
                                                    :desc         "Job 2"
                                                    :initialDelay 0
                                                    :msPeriod     20
                                                    :scheduled?   true}}
                                :status        "OK"}
                               (-> (mock/request :get "/status")
                                   (handler-fn)
                                   :body
                                   (json/read-str :key-fn keyword)
                                   (get-in [:application :statusDetails :scheduler])))))))))

(deftest ^:unit scheduler-default-conf
  (testing "should startup pool with core-pool-size of 0 if nothing else is configured"
    (u/with-started [system (serverless-system {})]
                    (let [{:keys [scheduler]} system
                          ^ScheduledThreadPoolExecutor thread-pool (:thread-pool @(:pool-atom (scheduler/pool scheduler)))]
                      (is (= 0 (.getCorePoolSize thread-pool))))))

  (testing "should startup pool with configured core-pool-size"
    (u/with-started [system (serverless-system {:scheduler {:cpu-count 2}})]
                    (let [{:keys [scheduler]} system
                          ^ScheduledThreadPoolExecutor thread-pool (:thread-pool @(:pool-atom (scheduler/pool scheduler)))]
                      (is (= 2 (.getCorePoolSize thread-pool)))))))
