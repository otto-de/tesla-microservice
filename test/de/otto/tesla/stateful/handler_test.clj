(ns de.otto.tesla.stateful.handler-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.handler :as handler]
            [com.stuartsierra.component :as c])
  (:import (com.codahale.metrics Timer)))

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

(def request-based-timer-id #'handler/request-based-timer-id)

(deftest building-timer-id
  (testing "should build path with first resource of uri"
    (let [item {:timed?          true
                :uri-resource-fn (partial take 1)}]
      (is (= ["base" "path" "foo" "200"]
             (request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {:status 200})))
      (is (= ["base" "path" "200"]
             (request-based-timer-id ["base" "path"] item {:uri "/?item=123"} {:status 200})))))

  (testing "should build path with first 2 resources of uri"
    (let [item {:timed?          true
                :uri-resource-fn (partial take 2)}]
      (is (= ["base" "path" "foo" "bar" "200"]
             (request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {:status 200})))
      (is (= ["base" "path" "200"]
             (request-based-timer-id ["base" "path"] item {:uri "/?item=123"} {:status 200})))))

  (testing "should build path with all but last resource of uri"
    (let [item {:timed?          true
                :uri-resource-fn butlast}]
      (is (= ["base" "path" "foo" "bar" "baz" "200"]
             (request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {:status 200})))
      (is (= ["base" "path" "foo" "bar" "baz" "baf" "bif" "200"]
             (request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf/bif/bum?item=123"} {:status 200})))
      (is (= ["base" "path" "200"]
             (request-based-timer-id ["base" "path"] item {:uri "/?item=123"} {:status 200}))))))

(deftest request-based-timer-id-with-status
  (testing "should build path with status code"
    (let [item {:timed?          true
                :uri-resource-fn (partial take 2)}]
      (is (= ["base" "path" "foo" "bar" "200"]
             (request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {:status 200})))
      (is (= ["base" "path" "foo" "bar" "404"]
             (request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {:status 404})))
      (is (= ["base" "path" "foo" "bar" "500"]
             (request-based-timer-id ["base" "path"] item {:uri "/foo/bar/baz/baf?item=123"} {:status 500}))))))

(def trimmed-uri-path #'handler/trimmed-uri-path)
(deftest trimmed-uri-path-test
  (testing "should trim uri path"
    (is (= "foo/bar/baz" (trimmed-uri-path "/foo/bar/baz?a=b&c=d")))
    (is (= nil (trimmed-uri-path "/?a=b&c=d")))))

(deftest reporting-base-path
  (testing "should use default reporting base-path"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [self _ _ _ _] (reset! reportings (:reporting-base-path self)))]
        (let [handler (-> (handler/->Handler {}) (c/start))]
          (handler/register-timed-handler handler (fn [r] (when (= r :ping) :pong)))
          (is (= :pong ((handler/handler handler) :ping)))
          (is (= ["serving" "requests"] @reportings))))))

  (testing "should use configured reporting base-path"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [self _ _ _ _] (reset! reportings (:reporting-base-path self)))]
        (let [handler (-> (handler/->Handler {:config {:handler {:reporting-base-path ["foo" "bar" "baz"]}}}) (c/start))]
          (handler/register-timed-handler handler (fn [r] (when (= r :ping) :pong)))
          (is (= :pong ((handler/handler handler) :ping)))
          (is (= ["foo" "bar" "baz"] @reportings)))))))


(deftest storing-timers
  (let [handler (-> (handler/->Handler {}) (c/start))
        timer-updates (atom [])
        mock-timer (proxy [Timer] [] (update [v _] (swap! timer-updates conj v)))
        custom-handler-fn (fn [r] (when (= (:uri r) "/ping") {:status 200 :body :pong}))]
    (with-redefs [handler/register-timer (constantly nil)
                  handler/time-taken (constantly 100)
                  handler/sliding-window-timer (constantly mock-timer)]

      (testing "should store registered timer"
        (handler/register-timed-handler handler custom-handler-fn)
        (is (= [{:handler         custom-handler-fn
                 :handler-name    "tesla-handler-0"
                 :timed?          true
                 :uri-resource-fn identity}]
               @(:registered-handlers handler))))

      (testing "should respond with valid response and store + update timer"
        (is (= {:status 200
                :body   :pong}
               ((handler/handler handler) {:uri "/ping"})))
        (is (= {"serving.requests.ping.200" mock-timer} @(:timers handler)))
        (is (= [100] @timer-updates)))

      (testing "should reuse timer for further updates"
        ((handler/handler handler) {:uri "/ping"})
        ((handler/handler handler) {:uri "/ping"})

        (is (= {"serving.requests.ping.200" mock-timer} @(:timers handler)))
        (is (= [100 100 100] @timer-updates))))))
