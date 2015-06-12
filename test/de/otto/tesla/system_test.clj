(ns de.otto.tesla.system-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as c]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]))


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