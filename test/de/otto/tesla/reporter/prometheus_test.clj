(ns de.otto.tesla.reporter.prometheus-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.system :as system]
            [com.stuartsierra.component :as c]
            [clojure.data.codec.base64 :as b64]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.stateful.handler :as handler]
            [ring.mock.request :as mock]))

(defn- to-base64 [original]
  (String. ^bytes (b64/encode (.getBytes original)) "UTF-8"))

(defn- auth-header [request user password]
  (mock/header request "authorization" (str "Basic " (to-base64 (str user ":" password)))))

(defn system [runtime-config auth-fn]
  (if auth-fn
    (dissoc (assoc (system/base-system runtime-config) :metering (c/using (metering/new-metering auth-fn) [:config :handler])) :server)
    (system/base-system runtime-config)))

(defn- handlers [runtime-config & [auth-fn]]
  (let [system (system runtime-config auth-fn)
        started-system (c/start-system system)]
    (handler/handler (:handler started-system))))

(def metrics-path "/metrics")

(defn- rc-status-request [system-handler user password]
  (-> (mock/request :get metrics-path)
      (auth-header user password)
      (system-handler)
      :status))

(deftest authentication
  (let [config {:metrics {:prometheus {:metrics-path metrics-path}}}
        user "some-user"
        password "some-password"
        auth-fn (fn [usr pw] (and (= user usr) (= password pw)))
        system-handler (handlers config auth-fn)]
    (testing "it should allow access if authentication succeeds"
      (is (= 200 (rc-status-request system-handler user password))))
    (testing "it should deny access if authentication fails"
      (is (= 401 (rc-status-request system-handler user "wrong password"))))))


