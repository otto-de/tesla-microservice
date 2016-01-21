(ns de.otto.tesla.stateful.metering-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.configuring :as configuring]
            [metrics.timers :as timers]))

(def graphite-host-prefix #'metering/graphite-host-prefix)
(deftest ^:unit graphite-prefix-test
  (testing "returns prefix for testhost"
    (with-redefs [configuring/external-hostname (constantly "testhost.example.com")]
      (is (= "a-prefix.testhost.example.com"
             (graphite-host-prefix {:config {:graphite-prefix "a-prefix"}})))
      (is (= "a-prefix.testhost"
             (graphite-host-prefix {:config {:graphite-shorten-hostname? true
                                             :graphite-prefix "a-prefix"}}))))))

(deftest ^:unit the-metrics-lib-accepts-a-vector-for-building-the-name
  (is (= (metrics.core/metric-name ["some.name.foo.bar"])
         "some.name.foo.bar"))
  (is (= (metrics.core/metric-name ["some" "name" "foo" "bar"])
         "some.name.foo.bar")))

(deftest ^:unit metrics-registry-should-contain-correct-names
  (u/with-started [started (dissoc (system/base-system {}) :server)]
                  (let [metering (:metering started)]
                    (metering/timer! metering "some.name.timer.bar")
                    (metering/gauge! metering #() "some.name.gauge.bar")
                    (metering/counter! metering "some.name.counter.bar")
                    (timers/timer ["direct.usage.timer"])
                    (let [names (.getNames (:registry metering))]
                      (is (true? (contains? names "some.name.timer.bar")))
                      (is (true? (contains? names "some.name.gauge.bar")))
                      (is (true? (contains? names "some.name.counter.bar")))
                      (is (true? (contains? names "direct.usage.timer")))))))

(def short-hostname #'metering/short-hostname)
(deftest short-hostname-test
  (testing "it only returns the important part of a full-qualified hostname"
    (is (= ""
           (short-hostname "")))
    (is (= "mesos-slave-dev-399946"
           (short-hostname "mesos-slave-dev-399946.lhotse.ov.otto.de")))))