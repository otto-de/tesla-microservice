(ns de.otto.tesla.stateful.configuring-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.configuring :as configuring]
            [com.stuartsierra.component :as component]
            [clojure.java.io :as io]
            [de.otto.tesla.util.test-utils :as u]
            [environ.core :as env]))

(defn- test-system [rt-conf]
  (-> (component/system-map
        :conf (configuring/new-config rt-conf))))

(deftest referencing-env-properties
  (testing "should return empty if env prop does not exist and fallback not provided"
    (with-redefs [env/env {}]
      (u/with-started [started (test-system {})
                       conf (get-in started [:conf :config])]
                      (is (= (:prop-without-fallback conf) ""))))))

(deftest ^:unit should-read-property-from-default-config
  (testing "should be possible to prefer reading configs from property files"
    (u/with-started [started (test-system {:property-file-preferred true})]
                    (let [conf (get-in started [:conf :config])]
                      (is (= (:foo-prop conf) "baz"))
                      (is (= (get-in conf [:foo :edn]) nil))))))

(deftest ^:unit should-read-property-from-default-edn-file
  (u/with-started [started (test-system {})]
                  (let [edn-conf (get-in started [:conf :config])]
                    (is (= (:foo-prop edn-conf) nil))
                    (is (= (get-in edn-conf [:foo :edn]) "baz")))))

(deftest ^:unit should-read-property-from-custom-edn-file
  (with-redefs [env/env {:config-file "test.edn"}]
    (u/with-started [started (test-system {})]
                    (let [edn-conf (get-in started [:conf :config])]
                      (is (= (get-in edn-conf [:health-url]) "/test/health"))))))

(deftest ^:unit should-read-property-from-runtime-config
  (u/with-started [started (test-system {:foo-rt "bat" :fooz {:nested 123}})]
                  (let [edn-conf (get-in started [:conf :config])]
                    (is (= (:foo-prop edn-conf) nil))
                    (is (= (:foo-rt edn-conf) "bat"))
                    (is (= (get-in edn-conf [:foo :edn]) "baz"))
                    (is (= (get-in edn-conf [:fooz :nested]) 123)))))

(deftest ^:unit should-read-default-properties
  (testing "should read default properties from property-files"
    (let [loaded-properties (configuring/load-config-from-property-files)]
      (is (not (nil? (:server-port loaded-properties))))
      (is (not (nil? (:metering-reporter loaded-properties))))))

  (testing "should read default properties from edn-property-files"
    (let [loaded-properties (configuring/load-config-from-edn-files)]
      (is (not (nil? (:server-port loaded-properties))))
      (is (not (nil? (:metering-reporter loaded-properties)))))))

(deftest ^:unit should-determine-hostname-from-properties-with-defined-precedence
  (testing "should prefer $HOST"
    (u/with-started [started (test-system {:host "host" :host-name "host-name" :hostname "hostname"})]
                    (is (= "host" (configuring/external-hostname (:conf started))))))
  (testing "should prefer $HOST_NAME"
    (u/with-started [started (test-system {:host-name "host-name" :hostname "hostname"})]
                    (is (= "host-name" (configuring/external-hostname (:conf started))))))
  (testing "should choose $HOSTNAME"
    (u/with-started [started (test-system {:hostname "hostname"})]
                    (is (= "hostname" (configuring/external-hostname (:conf started))))))
  (testing "should fallback to localhost"
    (u/with-started [started (test-system {})]
                    (is (= "localhost" (configuring/external-hostname (:conf started)))))))

(deftest ^:unit should-determine-hostport-from-properties-with-defined-precedence
  (testing "should prefer $PORT0"
    (u/with-started [started (test-system {:port0 "0" :host-port "1" :server-port "2"})]
                    (is (= "0" (configuring/external-port (:conf started))))))
  (testing "should prefer $HOST_PORT"
    (u/with-started [started (test-system {:host-port "1" :server-port "2"})]
                    (is (= "1" (configuring/external-port (:conf started))))))
  (testing "should fallback to $SERVER_PORT"
    (u/with-started [started (test-system {:server-port "2"})]
                    (is (= "2" (configuring/external-port (:conf started)))))))

(deftest ^:integration should-read-properties-from-file
  (spit "application.properties" "foooo=barrrr")
  (is (= (:foooo (configuring/load-config-from-property-files))
         "barrrr"))
  (io/delete-file "application.properties"))

(deftest ^:integration should-prefer-configured-conf-file
  (spit "application.properties" "foooo=value")
  (spit "other.properties" "foooo=other-value")
  (with-redefs-fn {#'env/env {:config-file "other.properties"}}
    #(is (= (:foooo (configuring/load-config-from-property-files))
            "other-value")))
  (io/delete-file "other.properties")
  (io/delete-file "application.properties"))
