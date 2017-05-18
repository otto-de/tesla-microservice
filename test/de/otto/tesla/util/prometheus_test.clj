(ns de.otto.tesla.util.prometheus-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.util.prometheus :as prom]
            [metrics.histograms :as hists]
            [metrics.core :as metrics]
            [metrics.counters :as counter]
            [metrics.meters :as meters]
            [metrics.timers :as timer]
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
  (testing "Should transform a histogram into their string representation"
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
  (testing "Should transform a counter into their string representation"
    (metrics/remove-all-metrics)
    (let [c (counter/inc! (counter/counter "foobar.counter2") 3)]
      (is (= (str "# TYPE counter2 counter\n"
                  "counter2 3\n")
             (prom/counter->text ["counter2" c]))))))

(deftest meters-transformation-test
  (testing "Should transform a meter into a string representation and map it to a prometheus counter"
    (metrics/remove-all-metrics)
    (let [m (meters/mark! (meters/meter "meter1"))]
      (is (= (str "# TYPE meter1 counter\n"
                  "meter1 1\n")
             (prom/counter->text ["meter1" m]))))))

(deftest timers-transformation-test
  (testing "Should transform a timer into a prometheus counter as well as in a prometheus summary"
    (metrics/remove-all-metrics)
    (let [t (timer/timer "timer1")]
      (is (= (str "# TYPE timer1_cnt counter\n"
                  "timer1_cnt 0\n"
                  "# TYPE timer1_hist summary\n"
                  "timer1_hist{quantile=\"0.01\"} 0.0\n"
                  "timer1_hist{quantile=\"0.05\"} 0.0\n"
                  "timer1_hist{quantile=\"0.5\"} 0.0\n"
                  "timer1_hist{quantile=\"0.9\"} 0.0\n"
                  "timer1_hist{quantile=\"0.99\"} 0.0\n"
                  "timer1_hist_sum 0\n"
                  "timer1_hist_count 0\n")
             (prom/timer->text ["timer1" t]))))))

(deftest gauges-transformation-test
  (testing "Should transform a gauge into a string representation"
    (metrics/remove-all-metrics)
    (let [g1 (gauges/gauge-fn "foobar.gauge1" (constantly 42))
          g-invalid (gauges/gauge-fn "foobar.gauge1" (constantly "No number"))]
      (gauges/gauge-fn "foobar.gauge2" (constantly 1337))
      (is (= (str "# TYPE gauge1 gauge\n"
                   "gauge1 42\n")
             (prom/gauge->text ["gauge1" g1])))
      (is (= nil
             (prom/gauge->text ["my_name" g-invalid]))))))
