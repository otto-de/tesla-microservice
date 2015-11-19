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

(deftest ^:unit should-read-property-from-default-config
  (testing "should be possible to prefer reading configs from property files"
  (u/with-started [started (test-system {:property-file-preferred true})]
                  (let [conf (:config (:conf started))]
                    (is (= (:foo-bar conf) "baz"))))))

(deftest ^:unit should-read-property-from-default-edn-file
  (u/with-started [started (test-system {})]
                  (let [edn-conf (:config (:conf started))]
                    (is (= (get-in edn-conf [:foo :bar]) "baz")))))

(deftest ^:unit should-read-property-from-runtime-config
  (u/with-started [started (test-system {:foo-bar "bak"})]
                  (let [conf (:config (:conf started))]
                    (is (= (:foo-bar conf) "bak")))))

(deftest ^:unit should-read-propper-keywords
  (testing "should read the cache-dir as propper sanatized keyowrd from config"
    (let [loaded-properties (configuring/load-config-from-property-files)]
      (is (not (empty? (:cache-dir loaded-properties))))))

  (testing "should read the metering-reporter as propper sanatized keyowrd from config"
    (let [loaded-properties (configuring/load-config-from-property-files)]
      (is (not (empty? (:metering-reporter loaded-properties)))))))

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


