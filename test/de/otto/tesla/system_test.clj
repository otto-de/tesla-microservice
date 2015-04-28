(ns de.otto.tesla.system-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as c]
            [de.otto.tesla.util.test-utils :as u]

            [de.otto.tesla.system :as system]))

(defn- serverless-system [runtime-config]
  (dissoc
    (system/empty-system runtime-config)
    :server))

(deftest ^:unit should-start-empty-system-and-shut-it-down
  (let [started (system/start-system (serverless-system {}))
        stopped (system/stop started) ]
    (is (= "look ma, no exceptions" "look ma, no exceptions"))))

(deftest ^:unit should-start-empty-system-and-shut-it-down-using-lib
  (let [started (system/start-system (serverless-system {}))
        stopped (c/stop started) ]
    (is (= "look ma, no exceptions" "look ma, no exceptions"))))

(deftest should-lock-application-on-shutdown
  (testing "the lock is set"
    (u/with-started
      [started (serverless-system {:wait-ms-on-stop 10})]
      (let [healthcomp (:health started)
            _ (system/stop started)]
        (is (= @(:locked healthcomp) true)))))

  (testing "it waits on stop"
    (u/with-started
      [started (serverless-system {:wait-seconds-on-stop 1})]
      (let [has-waited (atom false)]
        (with-redefs [system/wait! (fn [_] (reset! has-waited true))]
          (let [healthcomp (:health started)
                _ (system/stop started)]
            (is (= @(:locked healthcomp) true))
            (is (= @has-waited true)))))))

  )
