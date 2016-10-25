(ns de.otto.tesla.stateful.handler-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.handler :as handler]
            [com.stuartsierra.component :as c]))

(deftest registering-handlers
  (testing "should register a handler and return a single handling-function"
    (let [handler (-> (handler/new-handler) (c/start))]
      (handler/register-handler handler (fn [r] (when (= r :ping) :pong)))
      (handler/register-handler handler (fn [r] (when (= r :pong) :ping)))
      (is (= ["tesla-handler-0" "tesla-handler-1"] (map :handler-name @(:the-handlers handler))))
      (is (= :pong ((handler/handler handler) :ping)))
      (is (= :ping ((handler/handler handler) :pong)))))

  (testing "should register a handler with a name and return a single handling-function"
    (let [handler (-> (handler/new-handler) (c/start))]
      (handler/register-handler handler "ping" (fn [r] (when (= r :ping) :pong)))
      (handler/register-handler handler "pong" (fn [r] (when (= r :pong) :ping)))
      (is (= ["ping" "pong"] (map :handler-name @(:the-handlers handler))))
      (is (= :pong ((handler/handler handler) :ping)))
      (is (= :ping ((handler/handler handler) :pong))))))

(deftest timed-handler
  (testing "should use timed handler"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [_ handler-name time-taken] (swap! reportings conj [handler-name time-taken]))]
        (let [handler (-> (handler/->Handler {:config {:handler {:report-timings? true}}}) (c/start))]
          (handler/register-handler handler (fn [r] (when (= r :ping) :pong)))
          (handler/register-handler handler (fn [r] (when (= r :pong) :ping)))
          (is (= ["tesla-handler-0" "tesla-handler-1"] (map :handler-name @(:the-handlers handler))))
          (is (= :pong ((handler/handler handler) :ping)))
          (is (= [["tesla-handler-0" 100]] @reportings))
          (is (= :ping ((handler/handler handler) :pong)))
          (is (= [["tesla-handler-0" 100] ["tesla-handler-1" 100]] @reportings)))))))

(def timer-id handler/timer-id)
(deftest building-timer-id
  (testing "should build timer-id"
    (is (= ["a" "b" "c" "d"]
           (timer-id ["a" "b" "c"] "d")))
    (is (= ["d"]
           (timer-id [] "d")))))

(deftest reporting-base-path
  (testing "should use default reporting base-path"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [base-path _ _] (reset! reportings base-path))]
        (let [handler (-> (handler/->Handler {:config {:handler {:report-timings? true}}}) (c/start))]
          (handler/register-handler handler (fn [r] (when (= r :ping) :pong)))
          (is (= :pong ((handler/handler handler) :ping)))
          (is (= ["serving" "requests"] @reportings))))))

  (testing "should use configured reporting base-path"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [base-path _ _] (reset! reportings base-path))]
        (let [handler (-> (handler/->Handler {:config {:handler {:report-timings?     true
                                                                 :reporting-base-path ["foo" "bar" "baz"]}}}) (c/start))]
          (handler/register-handler handler (fn [r] (when (= r :ping) :pong)))
          (is (= :pong ((handler/handler handler) :ping)))
          (is (= ["foo" "bar" "baz"] @reportings)))))))
