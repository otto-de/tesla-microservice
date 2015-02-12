(ns de.otto.tesla.stateful.metering-test
  (:import (java.net UnknownHostException))
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]))
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

(deftest ^:unit the-metrics-lib-accepts-a-vector-for-building-the-name
  (is (= (metrics.core/metric-name ["some.name.foo.bar"])
         "some.name.foo.bar"))
  (is (= (metrics.core/metric-name ["some" "name" "foo" "bar"])
         "some.name.foo.bar")))

(deftest ^:unit metrics-registry-should-contain-correct-names
  (u/with-started [started (system/empty-system {})]
                  (let [metering (:metering started)]
                    (metering/timer! metering "some.name.timer.bar")
                    (metering/gauge! metering #() "some.name.gauge.bar")
                    (metering/counter! metering "some.name.counter.bar")
                    (let [names (.getNames (:registry metering))]
                      (is (true? (contains? names "some.name.timer.bar")))
                      (is (true? (contains? names "some.name.gauge.bar")))
                      (is (true? (contains? names "some.name.counter.bar")))))))
