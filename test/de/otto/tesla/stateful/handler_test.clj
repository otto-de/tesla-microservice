(ns de.otto.tesla.stateful.handler-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.handler :as handler]
            [com.stuartsierra.component :as component]
            [compojure.core :as compojure]
            [ring.util.response :as resp]
            [iapetos.registry :as ir]
            [ring.mock.request :as ring-mock]
            [de.otto.tesla.metrics.prometheus.core :as metrics])
  (:import (com.codahale.metrics Timer)))

(def ping->pong-route (fn [{:keys [uri]}] (when (= uri "/ping") {:body   :pong
                                                                 :status 200})))

(def pong->ping-route (fn [{:keys [uri]}] (when (= uri "/pong") {:body   :ping
                                                                 :status 200})))


(deftest registering-handlers
  (testing "should register a handler and return a single handling-function"
    (let [handler (-> (handler/new-handler) (component/start))]
      (handler/register-handler handler (fn [r] (when (= r :ping) :pong)))
      (handler/register-handler handler (fn [r] (when (= r :pong) :ping)))
      (is (= ["tesla-handler-0" "tesla-handler-1"] (map :handler-name @(:registered-handlers handler))))
      (is (= :pong ((handler/handler handler) :ping)))
      (is (= :ping ((handler/handler handler) :pong))))))

(deftest timed-handler
  (testing "should use timed handler"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [timer-id _ time-taken] (swap! reportings conj [timer-id time-taken]))]
        (let [handler (-> (handler/new-handler) (component/start))]
          (handler/register-timed-handler handler ping->pong-route)
          (handler/register-timed-handler handler pong->ping-route)
          (is (= ["tesla-handler-0" "tesla-handler-1"]
                 (map :handler-name @(:registered-handlers handler))))
          (is (= {:body :pong :status 200} ((handler/handler handler) {:uri "/ping"})))
          (is (= [[["serving" "requests" "ping" "200"] 100]] @reportings))
          (is (= {:body :ping :status 200} ((handler/handler handler) {:uri "/pong"})))
          (is (= [[["serving" "requests" "ping" "200"] 100]
                  [["serving" "requests" "pong" "200"] 100]] @reportings)))))))

(def request-based-timer-id #'handler/request-based-timer-id)

(deftest building-timer-id
  (testing "should build path with first resource of uri"
    (let [uri-resource-fn (partial take 1)]
      (is (= ["base" "path" "foo" "200"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/foo/bar/baz/baf?item=123"} {:status 200})))
      (is (= ["base" "path" "200"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/?item=123"} {:status 200})))))

  (testing "should build path with first 2 resources of uri"
    (let [uri-resource-fn (partial take 2)]
      (is (= ["base" "path" "foo" "bar" "200"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/foo/bar/baz/baf?item=123"} {:status 200})))
      (is (= ["base" "path" "200"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/?item=123"} {:status 200})))))

  (testing "should build path with all but last resource of uri"
    (let [uri-resource-fn butlast]
      (is (= ["base" "path" "foo" "bar" "baz" "200"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/foo/bar/baz/baf?item=123"} {:status 200})))
      (is (= ["base" "path" "foo" "bar" "baz" "baf" "bif" "200"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/foo/bar/baz/baf/bif/bum?item=123"} {:status 200})))
      (is (= ["base" "path" "200"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/?item=123"} {:status 200}))))))

(deftest request-based-timer-id-with-status
  (testing "should build path with status code"
    (let [uri-resource-fn (partial take 2)]
      (is (= ["base" "path" "foo" "bar" "200"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/foo/bar/baz/baf?item=123"} {:status 200})))
      (is (= ["base" "path" "foo" "bar" "404"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/foo/bar/baz/baf?item=123"} {:status 404})))
      (is (= ["base" "path" "foo" "bar" "500"]
             (request-based-timer-id ["base" "path"] uri-resource-fn {:uri "/foo/bar/baz/baf?item=123"} {:status 500}))))))

(def trimmed-uri-path #'handler/trimmed-uri-path)
(deftest trimmed-uri-path-test
  (testing "should trim uri path"
    (is (= "foo/bar/baz" (trimmed-uri-path "/foo/bar/baz?a=b&c=d")))
    (is (= nil (trimmed-uri-path "/?a=b&c=d")))))

(deftest reporting-base-path
  (testing "should use default reporting base-path"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [_ self _] (reset! reportings (:reporting-base-path self)))]
        (let [handler (-> (handler/->Handler {}) (component/start))]
          (handler/register-timed-handler handler ping->pong-route)
          (is (= {:body :pong :status 200} ((handler/handler handler) {:uri "/ping"})))
          (is (= ["serving" "requests"] @reportings))))))
  (testing "should use configured reporting base-path"
    (let [reportings (atom [])]
      (with-redefs [handler/time-taken (constantly 100)
                    handler/report-request-timings! (fn [_ self _] (reset! reportings (:reporting-base-path self)))]
        (let [handler (-> (handler/->Handler {:config {:handler {:reporting-base-path ["foo" "bar" "baz"]}}}) (component/start))]
          (handler/register-timed-handler handler ping->pong-route)
          (is (= {:body :pong :status 200} ((handler/handler handler) {:uri "/ping"})))
          (is (= ["foo" "bar" "baz"] @reportings)))))))

(deftest storing-timers
  (let [handler (-> (handler/->Handler {}) (component/start))
        timer-updates (atom [])
        mock-timer (proxy [Timer] [] (update [v _] (swap! timer-updates conj v)))]
    (with-redefs [handler/register-timer (constantly nil)
                  handler/time-taken (constantly 100)
                  handler/sliding-window-timer (constantly mock-timer)]

      (testing "should store registered handler"
        (handler/register-timed-handler handler ping->pong-route)
        (is (= {:handler      ping->pong-route
                :handler-name "tesla-handler-0"}
               (dissoc (first @(:registered-handlers handler)) :timer-path-fn)))
        (is (fn? (:timer-path-fn (first @(:registered-handlers handler))))))

      (testing "should respond with valid response and store + update timer"
        (is (= {:status 200 :body :pong} ((handler/handler handler) {:uri "/ping"})))
        (is (= {"serving.requests.ping.200" mock-timer} @(:timers handler)))
        (is (= [100] @timer-updates)))

      (testing "should reuse timer for further updates"
        ((handler/handler handler) {:uri "/ping"})
        ((handler/handler handler) {:uri "/ping"})

        (is (= {"serving.requests.ping.200" mock-timer} @(:timers handler)))
        (is (= [100 100 100] @timer-updates))))))

(def timer-for-id #'handler/timer-for-id)
(deftest stringifiy-timer-ids
  (testing "should stringify all timerid-arrays"
    (let [timers (atom {"1.2.3.4.5" :mock-timer})]
      (is (= :mock-timer (timer-for-id {:timers timers} ["1" "2" "3" "4" "5"]))))))

(deftest storing-timers-for-custom-timer-path-fn
  (let [handler (-> (handler/->Handler {}) (component/start))
        timer-updates (atom [])
        mock-timer (proxy [Timer] [] (update [v _] (swap! timer-updates conj v)))
        custom-timer-path-fn (fn [request response]
                               ["custom" (:status response) (get-in request [:headers :foo]) "handler"])]
    (with-redefs [handler/register-timer (constantly nil)
                  handler/time-taken (constantly 100)
                  handler/sliding-window-timer (constantly mock-timer)]

      (testing "should store registered timer"
        (handler/register-timed-handler handler ping->pong-route
                                        :timer-path-fn custom-timer-path-fn
                                        )
        (is (= [{:handler       ping->pong-route
                 :handler-name  "tesla-handler-0"
                 :timer-path-fn custom-timer-path-fn}]
               @(:registered-handlers handler))))

      (testing "should respond with valid response and store + update timer"
        (is (= {:status 200
                :body   :pong}
               ((handler/handler handler) {:uri "/ping" :headers {:foo "foo-header"}})))
        (is (= {"custom.200.foo-header.handler" mock-timer} @(:timers handler)))
        (is (= [100] @timer-updates)))

      (testing "should reuse timer for further updates"
        ((handler/handler handler) {:uri "/ping" :headers {:foo "foo-header"}})
        ((handler/handler handler) {:uri "/ping" :headers {:foo "bar-header"}})

        (is (= {"custom.200.foo-header.handler" mock-timer
                "custom.200.bar-header.handler" mock-timer} @(:timers handler)))
        (is (= [100 100 100] @timer-updates))))))

(def single-handler-fn #'handler/single-handler-fn)
(deftest the-single-handler-fn
  (let [reportings (atom [])]
    (with-redefs [handler/time-taken (constantly 100)
                  handler/report-request-timings! (fn [timer-id _ time-taken] (swap! reportings conj [timer-id time-taken]))]
      (testing "should execute the single handler-fn"
        (reset! reportings [])
        (let [registered-handlers (atom [{:handler (fn [r] {:status 200 :body :dummy-response})}])]
          (is (= {:status 200 :body :dummy-response}
                 ((single-handler-fn {:registered-handlers registered-handlers}) {:uri "/"})))
          (is (= []  @reportings))))

      (testing "should execute the single handler-fn"
        (reset! reportings [])
        (let [registered-handlers (atom [{:timer-path-fn (constantly ["testpath"])
                                          :handler       (fn [r] {:status 200 :body :dummy-response})}])]
          (is (= {:status 200 :body :dummy-response}
                 ((single-handler-fn {:registered-handlers registered-handlers}) {:uri "/"})))
          (is (= [[["testpath"] 100]] @reportings))))

      (testing "should execute the single handler-fn and stringify elements in vector"
        (reset! reportings [])
        (let [registered-handlers (atom [{:timer-path-fn (constantly [1 2 3])
                                          :handler       (fn [r] {:status 200 :body :dummy-response})}])]
          (is (= {:status 200 :body :dummy-response}
                 ((single-handler-fn {:registered-handlers registered-handlers}) {:uri "/"})))
          (is (= [[["1" "2" "3"] 100]] @reportings))))

      (testing "should execute the single handler-fn and report nothing if path-fn throws an exception"
        (reset! reportings [])
        (let [registered-handlers (atom [{:timer-path-fn (fn [_ _] (throw (RuntimeException. "TextException")))
                                          :handler       (fn [r] {:status 200 :body :dummy-response})}])]
          (is (= {:status 200 :body :dummy-response}
                 ((single-handler-fn {:registered-handlers registered-handlers}) {:uri "/"})))
          (is (= [] @reportings)))))))
