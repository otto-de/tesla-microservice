(ns de.otto.tesla.stateful.scheduler-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.scheduler :as schedule]
            [de.otto.tesla.util.test-utils :as u]
            [overtone.at-at :as at]
            [com.stuartsierra.component :as c]))

(defn- serverless-system [runtime-config]
  (-> (system/base-system runtime-config)
      (dissoc :server)
      (assoc :scheduler (c/using (schedule/new-scheduler) [:config]))))

(deftest ^:unit should-call-function-at-scheduled-rate
  (u/with-started [system (serverless-system {:host-name "bar" :server-port "0123"})]
                  (let [scheduler (:scheduler system)]
                    (testing "Function gets called every 10 ms"
                      (let [calls (atom 0)]
                        (schedule/schedule scheduler #(swap! calls inc) 10)
                        (Thread/sleep 25)
                        (is (= @calls 3))))

                    (testing "Function gets called every 10 ms AFTER the function last returned"
                      (let [calls (atom 0)]
                        (schedule/schedule scheduler #((Thread/sleep 10) (swap! calls inc)) 10 true)
                        (Thread/sleep 25)
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
                    at/mk-pool (assert-map-args! nil)]
        (u/with-started [system (serverless-system config)]
                        (is ()))))))
