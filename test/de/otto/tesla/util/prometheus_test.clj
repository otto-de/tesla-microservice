(ns de.otto.tesla.util.prometheus-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.util.prometheus :as prom]
            [metrics.histograms :as hists]
            [metrics.core :as metrics]
            [metrics.counters :as counter]
            [metrics.gauges :as gauges]))

(deftest collect-empty-metrics-test
  (testing "Should transform nothing if the metrics are empty"
    (is (= []
           (prom/transform-metrics [] prom/histogram->text))))

  (testing "Should collect nothing if the metrics are empty"
    (metrics/remove-all-metrics)
    (is (= ""
           (prom/collect-metrics metrics/default-registry)))))

(deftest collect-some-metrics-test
  (testing "Should collect metrics into a single string representation"
    (with-redefs [prom/transform-metrics (constantly ["foobar"])]
      (is (= "foobar\nfoobar\nfoobar\n"
             (prom/collect-metrics metrics/default-registry))))))

(deftest histograms-transformation-test
  (testing "Should transform two histograms into their string representation"
    (metrics/remove-all-metrics)
    (hists/update! (hists/histogram "foobar.hist1") 5)
    (hists/update! (hists/histogram "foobar.hist2") 7)
    (is (= ["# TYPE default.default.foobar.hist1 histogram"
            "default.default.foobar.hist1{quantile=0.01} 5.0"
            "default.default.foobar.hist1{quantile=0.05} 5.0"
            "default.default.foobar.hist1{quantile=0.5} 5.0"
            "default.default.foobar.hist1{quantile=0.9} 5.0"
            "default.default.foobar.hist1{quantile=0.99} 5.0"
            "default.default.foobar.hist1_sum 5"
            "default.default.foobar.hist1_count 1"
            "# TYPE default.default.foobar.hist2 histogram"
            "default.default.foobar.hist2{quantile=0.01} 7.0"
            "default.default.foobar.hist2{quantile=0.05} 7.0"
            "default.default.foobar.hist2{quantile=0.5} 7.0"
            "default.default.foobar.hist2{quantile=0.9} 7.0"
            "default.default.foobar.hist2{quantile=0.99} 7.0"
            "default.default.foobar.hist2_sum 7"
            "default.default.foobar.hist2_count 1"]
           (prom/transform-metrics (metrics/histograms metrics/default-registry) prom/histogram->text)))))

(deftest counters-transformation-test
  (testing "Should transform two counters into their string representation"
    (metrics/remove-all-metrics)
    (counter/inc! (counter/counter "foobar.counter1"))
    (counter/inc! (counter/counter "foobar.counter2") 3)
    (is (= ["# TYPE default.default.foobar.counter1 counter"
            "default.default.foobar.counter1 1"
            "# TYPE default.default.foobar.counter2 counter"
            "default.default.foobar.counter2 3"]
           (prom/transform-metrics (metrics/counters metrics/default-registry) prom/counter->text)))))

(deftest gauges-transformation-test
  (testing "Should transform two gauges into their string representation"
    (metrics/remove-all-metrics)
    (gauges/gauge-fn "foobar.gauge1" (constantly 42))
    (gauges/gauge-fn "foobar.gauge2" (constantly 1337))
    (is (= ["# TYPE default.default.foobar.gauge1 gauge"
            "default.default.foobar.gauge1 42"
            "# TYPE default.default.foobar.gauge2 gauge"
            "default.default.foobar.gauge2 1337"]
           (prom/transform-metrics (metrics/gauges metrics/default-registry) prom/gauge->text)))))
