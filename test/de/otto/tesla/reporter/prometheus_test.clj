(ns de.otto.tesla.reporter.prometheus-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.system :as system]
            [com.stuartsierra.component :as c]
            [clojure.data.codec.base64 :as b64]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.stateful.handler :as handler]
            [ring.mock.request :as mock]
            [ring.middleware.basic-authentication :as ba]))

(defn- to-base64 [original]
  (String. ^bytes (b64/encode (.getBytes original)) "UTF-8"))

(defn- auth-header [request user password]
  (mock/header request "authorization" (str "Basic " (to-base64 (str user ":" password)))))

(defn system [runtime-config auth-middleware]
  (-> (system/base-system runtime-config auth-middleware)
      (dissoc :server)))

(defn- handlers [runtime-config & [auth-middleware]]
  (let [system         (system runtime-config auth-middleware)
        started-system (c/start-system system)]
    (handler/handler (:handler started-system))))

(defn- rc-metrics-request [system-handler user password]
  (-> (mock/request :get "/metrics")
      (auth-header user password)
      (system-handler)
      :status))

(deftest authentication
  (let [config          {:metrics {:prometheus {:metrics-path "/metrics"}}}
        auth-fun        (fn [usr pw]
                          (and
                            (= "some-user" usr) (= "some-password" pw)))
        auth-middleware (fn [config_ handler]
                          (ba/wrap-basic-authentication handler auth-fun))
        system-handler  (handlers config auth-middleware)]
    (testing "it should allow access if authentication succeeds"
      (is (= 200 (rc-metrics-request system-handler "some-user" "some-password"))))
    (testing "it should deny access if authentication fails"
      (is (= 401 (rc-metrics-request system-handler "some-user" "wrong password"))))))


