(ns de.otto.tesla.stateful.scheduler-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.scheduler :as schedule]
            [de.otto.tesla.util.test-utils :as u]))

(defn- serverless-system [runtime-config]
  (->(dissoc
    (system/base-system runtime-config)
    :server)
     (assoc :scheduler (schedule/new-scheduler))))

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
                        (schedule/schedule scheduler #((Thread/sleep 10)(swap! calls inc)) 10 true)
                        (Thread/sleep 25)
                        (is (= @calls 1)))))))