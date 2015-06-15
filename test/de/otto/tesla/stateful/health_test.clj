(ns de.otto.tesla.stateful.health-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.health :as health]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.handler :as handler]
            [ring.mock.request :as mock]))

(defn- serverless-system [runtime-config]
  (dissoc
    (system/base-system runtime-config)
    :server))

(deftest ^:unit should-turn-unhealthy-when-locked
  (u/with-started [started (serverless-system {})]
                  (testing "it is still healthy when not yet locked"
                    (let [handlers (handler/handler (:handler started))
                          response (handlers (mock/request :get "/health"))]
                      (are [key value] (= value (get response key))
                                       :body "HEALTHY"
                                       :status 200
                                       )
                      (are [key value] (= value (get (:headers response) key))
                                       "Content-Type" "text/plain; charset=utf-8")
                      ))

                  (testing "when locked, it is unhealthy"
                    (let [handlers (handler/handler (:handler started))
                          _ (health/lock-application (:health started))
                          response (handlers (mock/request :get "/health"))
                          ]
                      (are [key value] (= value (get response key))
                                       :body "UNHEALTHY"
                                       :status 503
                                       )
                      (are [key value] (= value (get (:headers response) key))
                                       "Content-Type" "text/plain; charset=utf-8")))))

(deftest ^:integration should-serve-health-under-configured-url
  (testing "use the default url"
    (u/with-started [started (serverless-system {})]
                    (let [handlers (handler/handler (:handler started))]
                      (is (= (:body (handlers (mock/request :get "/health")))
                             "HEALTHY"
                             )))))

  (testing "use the configuration url"
    (u/with-started [started (serverless-system {:health {:path "/my-health"}})]
                    (let [handlers (handler/handler (:handler started))]
                      (is (= (:body (handlers (mock/request :get "/my-health"))
                               "HEALTHY"
                               )))))))






