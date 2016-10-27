(ns de.otto.tesla.stateful.keep-alive-test
  (:require
    [clojure.test :refer :all]
    [de.otto.tesla.stateful.keep-alive :as kalive]
    [de.otto.tesla.util.test-utils :refer [eventually]]
    [de.otto.tesla.util.test-utils :as u]))

(deftest starting-and-stopping-the-keepalive-component
  (testing "should not start and stop keepalive-thread"
    (let [entered? (atom false)
          exited? (atom false)]
      (with-redefs [kalive/enter-keep-alive (fn [] (reset! entered? true))
                    kalive/exit-keep-alive (fn [] (reset! exited? true))]
        (is (= false @entered?))
        (is (= false @exited?))
        (u/with-started [started-keepalive (kalive/new-keep-alive)]
                        (Thread/sleep 100) ;stay in started state for some time
                        (is (= true @entered?))
                        (is (= false @exited?)))
        (is (= true @entered?))
        (eventually (= true @exited?))))))
