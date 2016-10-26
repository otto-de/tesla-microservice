(ns de.otto.tesla.stateful.handler-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.handler :as handler]
            [com.stuartsierra.component :as c]))

(deftest registering-handlers
  (testing "should register a handler and return a single handling-function"
    (let [handler (-> (handler/new-handler) (c/start))]
      (handler/register-handler handler (fn [r] (when (= r :ping) :pong)))
      (handler/register-handler handler (fn [r] (when (= r :pong) :ping)))
      (is (= ["tesla-handler-0" "tesla-handler-1"] (map :handler-name @(:registered-handlers handler))))
      (is (= :pong ((handler/handler handler) :ping)))
      (is (= :ping ((handler/handler handler) :pong))))))

(deftest timed-handler
  (testing "should use timed handler"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [_ item _ _ time-taken] (swap! reportings conj [(:handler-name item) time-taken]))]
        (let [handler (-> (handler/new-handler) (c/start))]
          (handler/register-timed-handler handler (fn [r] (when (= r :ping) :pong)))
          (handler/register-timed-handler handler (fn [r] (when (= r :pong) :ping)))
          (is (= ["tesla-handler-0" "tesla-handler-1"]
                 (map :handler-name @(:registered-handlers handler))))
          (is (= :pong ((handler/handler handler) :ping)))
          (is (= [["tesla-handler-0" 100]] @reportings))
          (is (= :ping ((handler/handler handler) :pong)))
          (is (= [["tesla-handler-0" 100] ["tesla-handler-1" 100]] @reportings)))))))

(deftest building-timer-id
  (testing "should build path with first resource of uri"
    (let [item {:timed?                  true
                :uri-resource-chooser-fn (partial take 1)
                :use-status-codes?       false}]
      (is (= ["base" "path" "foo"]
             (handler/request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {})))))

  (testing "should build path with first 2 resources of uri"
    (let [item {:timed?                  true
                :uri-resource-chooser-fn (partial take 2)
                :use-status-codes?       false}]
      (is (= ["base" "path" "foo" "bar"]
             (handler/request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {})))))

  (testing "should build path with all but last resource of uri"
    (let [item {:timed?                  true
                :uri-resource-chooser-fn pop
                :use-status-codes?       false}]
      (is (= ["base" "path" "foo" "bar" "baz"]
             (handler/request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {})))
      (is (= ["base" "path" "foo" "bar" "baz" "baf" "bif"]
             (handler/request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf/bif/bum?item=123"} {}))))))

(deftest request-based-timer-id-with-status
  (testing "should build path with status code"
    (let [item {:timed?                  true
                :uri-resource-chooser-fn (partial take 2)
                :use-status-codes?       true}]
      (is (= ["base" "path" "foo" "bar" "200"]
             (handler/request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {:status 200})))
      (is (= ["base" "path" "foo" "bar" "404"]
             (handler/request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {:status 404})))
      (is (= ["base" "path" "foo" "bar" "500"]
             (handler/request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {:status 500}))))))

(deftest reporting-base-path
  (testing "should use default reporting base-path"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [base-path _ _ _ _] (reset! reportings base-path))]
        (let [handler (-> (handler/->Handler {:config {:handler {:report-timings? true}}}) (c/start))]
          (handler/register-timed-handler handler (fn [r] (when (= r :ping) :pong)))
          (is (= :pong ((handler/handler handler) :ping)))
          (is (= ["serving" "requests"] @reportings))))))

  (testing "should use configured reporting base-path"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [base-path _ _ _ _] (reset! reportings base-path))]
        (let [handler (-> (handler/->Handler {:config {:handler {:report-timings?     true
                                                                 :reporting-base-path ["foo" "bar" "baz"]}}}) (c/start))]
          (handler/register-timed-handler handler (fn [r] (when (= r :ping) :pong)))
          (is (= :pong ((handler/handler handler) :ping)))
          (is (= ["foo" "bar" "baz"] @reportings)))))))
