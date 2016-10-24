(ns de.otto.tesla.stateful.keep-alive-test
  (:require
    [clojure.test :refer :all]
    [de.otto.tesla.stateful.keep-alive :as kalive]
    [com.stuartsierra.component :as c]))

(deftest starting-and-stopping-the-keepalive-component
  (testing "should not start and stop keepalive-thread"
    (let [entered? (atom false)
          exited? (atom false)]
      (with-redefs [kalive/enter-keep-alive (fn [] (reset! entered? true))
                    kalive/exit-keep-alive (fn [] (reset! exited? true))]
        (is (= false @entered?))
        (is (= false @exited?))
        (let [started (-> (kalive/new-keep-alive)
                          (c/start))]
          (Thread/sleep 10)
          (is (= true @entered?))
          (is (= false @exited?))
          (c/stop started))
        (Thread/sleep 10)
        (is (= true @entered?))
        (is (= true @exited?))))))