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

(deftest ^:unit should-have-system-status-for-runtime-config
  (u/with-started [system (serverless-system {:host-name "bar" :server-port "0123"})]
                  (let [scheduler (:scheduler system)
                        calls (atom 0)]
                    (schedule/schedule scheduler #(swap! calls inc) 10)
                    (Thread/sleep 25)
                    (is (= @calls 3)))))