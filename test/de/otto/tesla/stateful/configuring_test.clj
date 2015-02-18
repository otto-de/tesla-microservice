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
  (u/with-started [started (test-system {})]
                  (let [conf (:config (:conf started))]
                    (is (= (:foo-bar conf) "baz")))))

(deftest ^:unit should-read-property-from-runtime-config
  (u/with-started [started (test-system {:foo-bar "bak"})]
                  (let [conf (:config (:conf started))]
                    (is (= (:foo-bar conf) "bak")))))

(deftest ^:unit should-read-propper-keywords
  (testing "should read the cache-dir as propper sanatized keyowrd from config"
    (let [loaded-properties (configuring/load-config)]
      (is (not (empty? (:cache-dir loaded-properties))))))

  (testing "should read the metering-reporter as propper sanatized keyowrd from config"
    (let [loaded-properties (configuring/load-config)]
      (is (not (empty? (:metering-reporter loaded-properties)))))))

(deftest ^:integration should-read-properties-from-file
  (spit "application.properties" "foooo=barrrr")
  (is (= (:foooo (configuring/load-config))
         "barrrr"))
  (io/delete-file "application.properties"))

(deftest ^:integration should-prefer-configured-conf-file
  (spit "application.properties" "foooo=value")
  (spit "other.properties" "foooo=other-value")
  (with-redefs-fn {#'env/env {:config-file "other.properties"}}
    #(is (= (:foooo (configuring/load-config))
            "other-value")))
  (io/delete-file "other.properties")
  (io/delete-file "application.properties"))

