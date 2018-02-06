(ns de.otto.tesla.reporter.prometheus-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.system :as system]
            [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.stateful.handler :as handler]
            [ring.mock.request :as mock]))


(defn- start-authenticated-system [runtime-config user password]
  (let [auth-fn (fn [usr pw] (and (= user usr) (= password pw)))
        metering-with-auth (c/using (metering/new-metering auth-fn) [:config :handler])
        system (-> (system/base-system runtime-config)
                   (dissoc :server)
                   (assoc :metering metering-with-auth))
        started-system (c/start-system system)]
    (handler/handler (:handler started-system))))

(deftest authentication
  (let [metrics-path "/metrics"
        handlers (start-authenticated-system {:metrics {:prometheus {:metrics-path metrics-path}}} "some-user" "some-password")]
    (testing "it should allow access if authentication succeeds"
      (is (= 200 (:status (handlers (mock/header (mock/request :get metrics-path) "authorization" "Basic c29tZS11c2VyOnNvbWUtcGFzc3dvcmQ="))))))
    (testing "it should deny access if authentication fails"
      (is (= 401 (:status (handlers (mock/header (mock/request :get metrics-path) "authorization" "Basic c29tZS11c2VyOnNvbWUtcGEzc3dvcmQ="))))))))


