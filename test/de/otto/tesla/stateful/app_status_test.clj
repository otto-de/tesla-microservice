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
  (u/with-started [system (serverless-system {:host-name "bar" :server-port "0123"})]
                  (let [status (:app-status system)
                        system-status (:system (app-status/status-response-body status))]
                    (is (= (:hostname system-status) "bar"))
                    (is (= (:port system-status) "0123"))
                    (is (not (nil? (:systemTime system-status)))))))

(deftest ^:unit host-name-and-port-on-app-status
  (with-redefs [env/env {:host-name "foo" :server-port "1234"}]
    (testing "should add host and port from env to app-status in property-file case"
      (u/with-started [system (serverless-system {:property-file-preferred true :merge-env-to-properties-config true})]
                      (let [status (:app-status system)
                            system-status (:system (app-status/status-response-body status))]
                        (is (= (:hostname system-status) "foo"))
                        (is (= (:port system-status) "1234"))
                        (is (not (nil? (:systemTime system-status)))))))
    (testing "should add host and port from env to app-status in edn-file case"
      (u/with-started [system (serverless-system {})]
                      (let [status (:app-status system)
                            system-status (:system (app-status/status-response-body status))]
                        (is (= (:hostname system-status) "foo"))
                        (is (= (:port system-status) "9991"))
                        (is (not (nil? (:systemTime system-status)))))))))

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
                        page (app-status/status-response status {})
                        _ (log/info page)
                        application-body (get (json/read-str (:body page)) "application")]
                    (testing "it shows OK as application status"
                      (is (= (get application-body "status")
                             "OK")))

                    (testing "it shows the substatus"
                      (is (= (get application-body "statusDetails")
                             {"mock"      {"message" "nevermind"
                                           "status"  "OK"}
                              "scheduler" {"poolInfo"      {"active"    0
                                                            "poolSize"  0
                                                            "queueSize" 0}
                                           "scheduledJobs" {}
                                           "status"        "OK"}}))))))

(deftest ^:unit should-show-warning-as-application-status
  (u/with-started [started (mock-status-system {:mock {:status  :warning
                                                       :message "nevermind"}})]
                  (let [status (:app-status started)
                        page (app-status/status-response status {})
                        applicationStatus (get (get (json/read-str (:body page)) "application") "status")]
                    (is (= applicationStatus "WARNING")))))


(deftest ^:integration should-serve-status-under-configured-url
  (testing "use the default url"
    (u/with-started [started (serverless-system {})]
                    (let [handlers (handler/handler (:handler started))]
                      (is (= (:status (handlers (mock/request :get "/status")))
                             200)))))

  (testing "use the configuration url"
    (u/with-started [started (serverless-system {:status-url "/my-status"})]
                    (let [handlers (handler/handler (:handler started))]
                      (is (= (:status (handlers (mock/request :get "/my-status")))
                             200)))))

  (testing "default should be overridden"
    (u/with-started [started (serverless-system {:status-url "/my-status"})]
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
    (let [config {:status-aggregation nil}]
      (is (= (app-status/aggregation-strategy config) s/strict-strategy))))

  (testing "it should use forgiving stategy if forgiving is configured"
    (let [config {:status-aggregation "forgiving"}]
      (is (= (app-status/aggregation-strategy config) s/forgiving-strategy))))

  (testing "it should use strict stategy if something else is configured"
    (let [config {:status-aggregation "unknown"}]
      (is (= (app-status/aggregation-strategy config) s/strict-strategy)))))