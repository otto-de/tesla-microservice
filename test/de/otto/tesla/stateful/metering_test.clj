(ns de.otto.tesla.stateful.metering-test
  (:import (java.net UnknownHostException))
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.metering :as metering]))

(def config
  {:graphite-prefix "a_random_prefix"})

(deftest ^:unit should-return-prefix-for-testhost
  (with-redefs-fn {#'metering/hostname-from-os (fn [] "testhost")}
                  #(is (= (metering/prefix config)
                          "a_random_prefix.testhost"))))

(deftest ^:unit should-return-default-prefix-if-hostname-is-not-found
  (with-redefs-fn {#'metering/hostname-from-os (fn [] (throw (UnknownHostException. "testException")))}
                  #(is (= (metering/prefix config)
                          "a_random_prefix.default"))))
