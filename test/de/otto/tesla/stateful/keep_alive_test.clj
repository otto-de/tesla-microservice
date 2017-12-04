(ns de.otto.tesla.stateful.keep-alive-test
  (:require
    [clojure.test :refer :all]
    [de.otto.tesla.stateful.keep-alive :as kalive]
    [de.otto.tesla.util.test-utils :refer [eventually]]
    [de.otto.tesla.util.test-utils :as u]
    [clojure.tools.logging :as log]))

(deftest starting-and-stopping-the-keepalive-component
  (testing "should start and stop keepalive-thread"
    (let [state (atom :not-started)]
      (with-redefs [kalive/enter-keep-alive (fn []
                                              (log/info "ENTER test keepalive")
                                              (reset! state :entered))
                    kalive/exit-keep-alive (fn []
                                             (log/info "EXIT test keepalive")
                                             (reset! state :exited))]
        (is (= :not-started @state))
        (u/with-started [_ (kalive/new-keep-alive)]
                        (eventually (= :entered @state)))
        (eventually (= :exited @state))))))
