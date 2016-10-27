(ns de.otto.tesla.util.test-utils-test
  (:require
    [clojure.test :refer :all]
    [de.otto.tesla.util.test-utils :as u]
    [com.stuartsierra.component :as c]))

(deftest eventually
  (testing "should eventually get the right random number"
    (u/eventually (= 1 (rand-int 2))))

  (testing "should eventually return the right response"
    (let [start-time (System/currentTimeMillis)]
      (u/eventually (= :expected-response (#(let [waited-200msec? (> (- (System/currentTimeMillis) start-time) 200)]
                                             (if waited-200msec?
                                               :expected-response
                                               :wrong-response))))))))

(defrecord StartableComponent [state]
  c/Lifecycle
  (start [self] (reset! state :started) self)
  (stop [self] (reset! state :stopped) self))

(deftest with-started
  (testing "should start and stop something"
    (let [state (atom :not-running)]
      (is (= :not-running @state))
      (u/with-started [started (->StartableComponent state)]
                      (is (= :started @state)))
      (is (= :stopped @state)))))

(deftest testing-the-mock-request
  (testing "should create mock-request"
    (is (= (u/mock-request :get "url" {})
           {:headers        {"host" "localhost"}
            :query-string   nil
            :remote-addr    "localhost"
            :request-method :get
            :scheme         :http
            :server-name    "localhost"
            :server-port    80
            :uri            "url"})))
  (testing "should create mock-request"
    (is (= (u/mock-request :get "url" {:headers {"content-type" "application/json"}})
           {:headers        {"host"         "localhost"
                             "content-type" "application/json"}
            :query-string   nil
            :remote-addr    "localhost"
            :request-method :get
            :scheme         :http
            :server-name    "localhost"
            :server-port    80
            :uri            "url"}))))
