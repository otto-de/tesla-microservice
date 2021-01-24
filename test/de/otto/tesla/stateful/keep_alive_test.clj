(ns de.otto.tesla.stateful.keep-alive-test
  (:require
    [clojure.test :refer [deftest testing is]]
    [de.otto.tesla.stateful.keep-alive :as kalive]
    [de.otto.tesla.util.test-utils :refer [eventually with-started]]
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
        (with-started [_ (kalive/new-keep-alive)]
          (eventually (= :entered @state)))
        (eventually (= :exited @state))))))
