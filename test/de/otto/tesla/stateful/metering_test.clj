(ns de.otto.tesla.stateful.metering-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.configuring :as configuring]
            [metrics.timers :as timers]
            [metrics.meters :as meters]
            [metrics.histograms :as hists]
            [metrics.counters :as counter]
            [metrics.gauges :as gauges]
            [metrics.core :as metrics]))

(def graphite-host-prefix #'metering/graphite-host-prefix)
(deftest ^:unit graphite-prefix-test
  (testing "returns prefix for testhost"
    (with-redefs [configuring/external-hostname (constantly "testhost.example.com")]
      (is (= "a-prefix.testhost.example.com"
             (graphite-host-prefix {:config {:graphite-prefix "a-prefix"}})))
      (is (= "a-prefix.testhost"
             (graphite-host-prefix {:config {:graphite-shorten-hostname? true
                                             :graphite-prefix            "a-prefix"}}))))))

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
                    (metering/histogram! metering "some.name.histogram.bar")
                    (timers/timer ["direct.usage.timer"])
                    (let [names (.getNames (:registry metering))]
                      (is (true? (contains? names "some.name.timer.bar")))
                      (is (true? (contains? names "some.name.gauge.bar")))
                      (is (true? (contains? names "some.name.counter.bar")))
                      (is (true? (contains? names "some.name.histogram.bar")))
                      (is (true? (contains? names "direct.usage.timer")))))))

(def short-hostname #'metering/short-hostname)
(deftest short-hostname-test
  (testing "it only returns the important part of a full-qualified hostname"
    (is (= ""
           (short-hostname "")))
    (is (= "mesos-slave-dev-399946"
           (short-hostname "mesos-slave-dev-399946.lhotse.ov.otto.de")))))

(deftest metered-execution-test
  (let [timer-started (atom 0)
        timer-stoped (atom 0)
        meters-marked (atom 0)]
    (with-redefs [meters/mark! (fn [_] (swap! meters-marked inc))
                  timers/start (fn [_] (swap! timer-started inc))
                  timers/stop (fn [_] (swap! timer-stoped inc))]
      (testing "it calls the given fn with provided params"
        (let [fn-to-call (fn [& all-params] (reverse all-params))]
          (is (= [:c :b :a]
                 (metering/metered-execution "prefix" fn-to-call :a :b :c)))))

      (reset! timer-started 0)
      (reset! timer-stoped 0)
      (reset! meters-marked 0)

      (testing "it starts and stops timer-started timer"
        (metering/metered-execution "prefix" identity :x)
        (is (= 1 @timer-started))
        (is (= 1 @timer-stoped))
        (is (= 1 @meters-marked)))

      (testing "it increments the exeception counter on exceptions"
        (let [fn-to-call (fn [_] (throw (Exception. "Moep")))]
          (try
            (metering/metered-execution "prefix" fn-to-call :x)
            (catch Exception _))
          (is (= 2 @meters-marked))))

      (reset! timer-started 0)
      (reset! timer-stoped 0)
      (reset! meters-marked 0))))

(def expected-metrics-endpoint-response
  {:body    "# TYPE default.default.test.gauge1 gauge
default.default.test.gauge1 42
# TYPE default.default.test.hist1 histogram
default.default.test.hist1{quantile=0.01} 5.0
default.default.test.hist1{quantile=0.05} 5.0
default.default.test.hist1{quantile=0.5} 7.0
default.default.test.hist1{quantile=0.9} 7.0
default.default.test.hist1{quantile=0.99} 7.0
default.default.test.hist1_sum 12
default.default.test.hist1_count 2
# TYPE default.default.test.counter1 counter
default.default.test.counter1 1
# TYPE default.default.test.counter2 counter
default.default.test.counter2 2
"
   :headers {"Content-Type" "application/json"}
   :status  200})

(deftest metrics-endpoint-test
  (testing "Should output all aggregated metrics in a prometheus readable representation"
    (u/with-started [started (system/base-system {})]
                    (metrics/remove-all-metrics)
                    (gauges/gauge-fn "test.gauge1" (constantly 42))
                    (counter/inc! (counter/counter "test.counter1"))
                    (counter/inc! (counter/counter "test.counter2") 2)
                    (hists/update! (hists/histogram "test.hist1") 5)
                    (hists/update! (hists/histogram "test.hist1") 7)
                    (is (= expected-metrics-endpoint-response
                           (metering/metrics-response (:metering started)))))))
