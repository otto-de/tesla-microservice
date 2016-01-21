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
  (testing "should return env-property if referenced in edn-config"
    (with-redefs [env/env {:prop-without-fallback "prop-value"}]
      (u/with-started [started (test-system {})
                       conf (get-in started [:conf :config])]
                      (is (= (:prop-without-fallback conf) "prop-value")))))
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

(deftest ^:unit determine-hostname-from-config-and-env-with-defined-precedence
  (testing "it prefers a explicitly configured :host-name"
    (with-redefs [env/env {:host "host" :host-name "host-name" :hostname "hostname"}]
      (u/with-started [started (test-system {:host-name "configured"})]
                      (is (= "configured"
                             (configuring/external-hostname (:conf started)))))))
  (testing "it falls back to env-vars and prefers $HOST"
    (with-redefs [env/env {:host "host" :host-name "host-name" :hostname "hostname"}]
      (u/with-started [started (test-system {})]
                      (is (= "host"
                             (configuring/external-hostname (:conf started)))))))
  (testing "it falls back to env-vars and prefers $HOST_NAME"
    (with-redefs [env/env {:host-name "host-name" :hostname "hostname"}]
      (u/with-started [started (test-system {})]
                      (is (= "host-name"
                             (configuring/external-hostname (:conf started)))))))
  (testing "it falls back to env-vars and looks finally for $HOSTNAME"
    (with-redefs [env/env {:hostname "hostname"}]
      (u/with-started [started (test-system {})]
                      (is (= "hostname"
                             (configuring/external-hostname (:conf started)))))))
  (testing "it eventually falls back to localhost"
    (u/with-started [started (test-system {})]
                    (is (= "localhost"
                           (configuring/external-hostname (:conf started)))))))

(deftest ^:unit determine-hostport-from-config-and-env-with-defined-precedence
  (with-redefs [configuring/load-config-from-edn-files (constantly {})]
    (testing "it prefers a explicitly configured :hostname"
      (with-redefs [env/env {:port0 "0" :host-port "1" :server-port "2"}]
        (u/with-started [started (test-system {:server-port "configured"})]
                        (is (= "configured"
                               (configuring/external-port (:conf started)))))))
    (testing "it falls back to env-vars and prefers $PORT0"
      (with-redefs [env/env {:port0 "0" :host-port "1" :server-port "2"}]
        (u/with-started [started (test-system {})]
                        (is (= "0"
                               (configuring/external-port (:conf started)))))))
    (testing "it falls back to env-vars and prefers $HOST_PORT"
      (with-redefs [env/env {:host-port "1" :server-port "2"}]
        (u/with-started [started (test-system {})]
                        (is (= "1"
                               (configuring/external-port (:conf started)))))))
    (testing "it falls back to env-vars and finally takes $SERVER_PORT"
      (with-redefs [env/env {:server-port "2"}]
        (u/with-started [started (test-system {})]
                        (is (= "2"
                               (configuring/external-port (:conf started)))))))))

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
