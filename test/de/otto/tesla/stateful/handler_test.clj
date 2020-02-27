(ns de.otto.tesla.stateful.handler-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.handler :as handler]
            [com.stuartsierra.component :as component]))

(deftest registering-handlers
  (testing "should register a handler and return a single handling-function"
    (let [handler (-> (handler/new-handler) (component/start))]
      (handler/register-handler handler (fn [r] (when (= r :ping) :pong)))
      (handler/register-handler handler (fn [r] (when (= r :pong) :ping)))
      (is (= ["tesla-handler-0" "tesla-handler-1"] (map :handler-name @(:registered-handlers handler))))
      (is (= :pong ((handler/handler handler) :ping)))
      (is (= :ping ((handler/handler handler) :pong))))))


