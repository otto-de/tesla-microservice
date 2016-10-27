(ns de.otto.tesla.stateful.keep-alive-test
  (:require
    [clojure.test :refer :all]
    [de.otto.tesla.stateful.keep-alive :as kalive]
    [com.stuartsierra.component :as c]
    [de.otto.tesla.util.test-utils :refer [eventually]]))

(deftest starting-and-stopping-the-keepalive-component
  (testing "should not start and stop keepalive-thread"
    (let [entered? (atom false)
          exited? (atom false)]
      (with-redefs [kalive/enter-keep-alive (fn [] (reset! entered? true))
                    kalive/exit-keep-alive (fn [] (reset! exited? true))]
        (eventually (= false @entered?))
        (eventually (= false @exited?))
        (let [started (-> (kalive/new-keep-alive)
                          (c/start))]
          (eventually (= true @entered?))
          (eventually (= false @exited?))
          (c/stop started))
        (eventually (= true @entered?))
        (eventually (= true @exited?))))))
