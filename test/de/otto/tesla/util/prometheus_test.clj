(ns de.otto.tesla.util.prometheus-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.util.prometheus :as prom]
            [metrics.histograms :as hists]
            [metrics.core :as metrics]
            [metrics.counters :as counter]
            [metrics.gauges :as gauges]))

(deftest collect-empty-metrics-test
  (testing "Should collect nothing if the metrics are empty"
    (metrics/remove-all-metrics)
    (is (= ""
           (prom/collect-metrics metrics/default-registry)))))

(deftest collect-some-metrics-test
  (metrics/remove-all-metrics)
  (testing "Should collect metrics into a single string representation"
    (with-redefs [metrics/counters (constantly {"counter1" (counter/counter "counter1")})
                  metrics/gauges (constantly {"gauge1" (gauges/gauge-fn "gauge1" (constantly 42))})
                  metrics/histograms (constantly {"hist1" (hists/histogram "hist1")})]
      (is (= (str "# TYPE counter1 counter\n"
                  "counter1 0\n"
                  "# TYPE hist1 summary\n"
                  "hist1{quantile=\"0.01\"} 0.0\n"
                  "hist1{quantile=\"0.05\"} 0.0\n"
                  "hist1{quantile=\"0.5\"} 0.0\n"
                  "hist1{quantile=\"0.9\"} 0.0\n"
                  "hist1{quantile=\"0.99\"} 0.0\n"
                  "hist1_sum 0\n"
                  "hist1_count 0\n"
                  "# TYPE gauge1 gauge\n"
                  "gauge1 42\n")
             (prom/collect-metrics metrics/default-registry))))))

(deftest histograms-transformation-test
  (testing "Should transform two histograms into their string representation"
    (metrics/remove-all-metrics)
    (let [h (hists/update! (hists/histogram "foobar.hist1") 5)]
      (is (= (str "# TYPE hist1 summary\n"
                  "hist1{quantile=\"0.01\"} 5.0\n"
                  "hist1{quantile=\"0.05\"} 5.0\n"
                  "hist1{quantile=\"0.5\"} 5.0\n"
                  "hist1{quantile=\"0.9\"} 5.0\n"
                  "hist1{quantile=\"0.99\"} 5.0\n"
                  "hist1_sum 5\n"
                  "hist1_count 1\n")
             (prom/histogram->text ["hist1" h]))))))

(deftest counters-transformation-test
  (testing "Should transform two counters into their string representation"
    (metrics/remove-all-metrics)
    (let [c (counter/inc! (counter/counter "foobar.counter2") 3)]
      (is (= (str "# TYPE counter2 counter\n"
                  "counter2 3\n")
             (prom/counter->text ["counter2" c]))))))

(deftest gauges-transformation-test
  (testing "Should transform two gauges into their string representation"
    (metrics/remove-all-metrics)
    (let [g1 (gauges/gauge-fn "foobar.gauge1" (constantly 42))
          g-invalid (gauges/gauge-fn "foobar.gauge1" (constantly "No number"))]
      (gauges/gauge-fn "foobar.gauge2" (constantly 1337))
      (is (= (str "# TYPE gauge1 gauge\n"
                   "gauge1 42\n")
             (prom/gauge->text ["gauge1" g1])))
      (is (= nil
             (prom/gauge->text ["my_name" g-invalid]))))))
