(ns de.otto.tesla.stateful.health-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.health :as health]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.routes :as rts]
            [ring.mock.request :as mock]
            [de.otto.status :as s]))

(defn- serverless-system [runtime-config]
  (dissoc
    (system/empty-system runtime-config)
    :server))

(deftest should-turn-unhealthy-when-locked
  (u/with-started [started (serverless-system {})]
                  (testing "it is still healthy when not yet locked"
                    (let [handlers (rts/routes (:routes started))]
                      (is (= (handlers (mock/request :get "/health"))
                             {:body    "HEALTHY"
                              :headers {"Content-Type" "text/plain"}
                              :status  200}))))

                  (testing "when locked, it is unhealthy"
                    (let [handlers (rts/routes (:routes started))
                          _ (health/lock-application (:health started))]
                      (is (= (handlers (mock/request :get "/health"))
                             {:body    "UNHEALTHY"
                              :headers {"Content-Type" "text/plain"}
                              :status  503}))))))

(deftest ^:integration should-serve-health-under-configured-url
  (testing "use the default url"
    (u/with-started [started (serverless-system {})]
                    (let [handlers (rts/routes (:routes started))]
                      (is (= (handlers (mock/request :get "/health"))
                             {:body    "HEALTHY"
                              :headers {"Content-Type" "text/plain"}
                              :status  200})))))

  (testing "use the configuration url"
    (u/with-started [started (serverless-system {:health-url "/my-health"})]
                    (let [handlers (rts/routes (:routes started))]
                      (is (= (handlers (mock/request :get "/my-health"))
                             {:body    "HEALTHY"
                              :headers {"Content-Type" "text/plain"}
                              :status  200}))))))






