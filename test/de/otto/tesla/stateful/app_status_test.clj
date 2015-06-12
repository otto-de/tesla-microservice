(ns de.otto.tesla.stateful.app-status-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.app-status :as app-status]
            [com.stuartsierra.component :as c]
            [environ.core :as env]
            [clojure.data.json :as json]
            [clojure.tools.logging :as log]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.handler :as handler]
            [ring.mock.request :as mock]
            [de.otto.status :as s]))

(defn- serverless-system [runtime-config]
  (dissoc
    (system/base-system runtime-config)
    :server))

(deftest ^:unit should-have-system-status-for-runtime-config
  (u/with-started [system (serverless-system {:hostname "bar" :external-port "0123"})]
                  (let [status (:app-status system)
                        system-status (:system (app-status/status-response-body status))]
                    (is (= (:hostname system-status) "bar"))
                    (is (= (:port system-status) "0123"))
                    (is (not (nil? (:systemTime system-status)))))))

(deftest ^:unit should-sanitize-passwords
  (is (= (app-status/sanitize {:somerandomstuff                        "not-so-secret"
                               :somerandomstuff-passwd-somerandomstuff "secret"
                               :somerandomstuff-pwd-somerandomstuff    "secret"} ["passwd" "pwd"])
         {:somerandomstuff                        "not-so-secret"
          :somerandomstuff-passwd-somerandomstuff "******"
          :somerandomstuff-pwd-somerandomstuff    "******"})))

(defrecord MockStatusSource [response]
  c/Lifecycle
  (start [self]
    (app-status/register-status-fun (:app-status self) #(:response self))
    self)
  (stop [self]
    self))

(defn- mock-status-system [response]
  (assoc (serverless-system {})
    :mock-status
    (c/using (map->MockStatusSource {:response response}) [:app-status])))

(deftest ^:unit should-show-applicationstatus
  (u/with-started [started (mock-status-system {:mock {:status  :ok
                                                       :message "nevermind"}})]
                  (let [status (:app-status started)
                        page (app-status/status-response status)
                        _ (log/info page)
                        application-body (get (json/read-str (:body page)) "application")]
                    (testing "it shows OK as application status"
                      (is (= (get application-body "status")
                             "OK")))

                    (testing "it shows the substatus"
                      (is (= (get application-body "statusDetails")
                             {"mock" {"message" "nevermind" "status" "OK"}}))))))

(deftest ^:unit should-show-warning-as-application-status
  (u/with-started [started (mock-status-system {:mock {:status  :warning
                                                       :message "nevermind"}})]
                  (let [status (:app-status started)
                        page (app-status/status-response status)
                        applicationStatus (get (get (json/read-str (:body page)) "application") "status")]
                    (is (= applicationStatus "WARNING")))))


(deftest ^:integration should-serve-status-under-configured-url
  (testing "use the default url"
    (u/with-started [started (serverless-system {})]
                    (let [handlers (handler/handler (:handler started))]
                      (is (= (:status (handlers (mock/request :get "/status")))
                             200)))))

  (testing "use the configuration url"
    (u/with-started [started (serverless-system {:status {:path "/my-status"}})]
                    (let [handlers (handler/handler (:handler started))]
                      (is (= (:status (handlers (mock/request :get "/my-status")))
                             200)))))

  (testing "default should be overridden"
    (u/with-started [started (serverless-system {:status {:path "/my-status"}})]
                    (let [handlers (handler/handler (:handler started))]
                      (is (= (handlers (mock/request :get "/status"))
                             nil))))))

(deftest should-add-version-properties-to-status
  (testing "it should add the version properties"
    (u/with-started [started (serverless-system {})]
                    (let [handlers (handler/handler (:handler started))
                          request (mock/request :get "/status")
                          status-map (json/read-json (:body (handlers request)))]
                      (is (= (get-in status-map [:application :version]) "test.version"))
                      (is (= (get-in status-map [:application :git]) "test.githash"))))))

(deftest determine-status-strategy
  (testing "it should use strict stategy if none is configured"
    (let [config {:config {:status-aggregation nil}}]
      (is (= (app-status/aggregation-strategy config) s/strict-strategy))))

  (testing "it should use forgiving stategy if forgiving is configured"
    (let [config {:config {:status-aggregation "forgiving"}}]
      (is (= (app-status/aggregation-strategy config) s/forgiving-strategy))))

  (testing "it should use strict stategy if something else is configured"
    (let [config {:config {:status-aggregation "unknown"}}]
      (is (= (app-status/aggregation-strategy config) s/strict-strategy)))))