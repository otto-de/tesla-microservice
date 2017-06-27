(ns de.otto.tesla.metrics.prometheus.graphite-test
  (:require
    [clojure.test :refer :all]
    [de.otto.tesla.metrics.prometheus.graphite :as graphite]
    [de.otto.tesla.metrics.prometheus.core :as metrics]
    [iapetos.core :as p]
    [clojure.string :as cs]))

(defn to-graphite [prefix]
  (let [writer (atom "")]
    (with-redefs [graphite/now-in-s (constantly 1111)]
      (#'graphite/write-metrics! #(swap! writer str %) prefix (metrics/snapshot))
      @writer)))

(deftest report-to-buffer-test
  (testing "report empty registry"
    (metrics/clear-default-registry!)
    (is (= "" (to-graphite ""))))

  (testing "report registry with a counter"
    (metrics/clear-default-registry!)
    (metrics/register+execute :ns/cnt (p/counter {}) (p/inc {}))
    (is (= "ns_cnt 1.0 1111\n"
           (to-graphite ""))))

  (testing "report registry with a counter with a prefix"
    (metrics/clear-default-registry!)
    (metrics/register+execute :ns/cnt (p/counter {:labels [:my-label]}) (p/inc {:my-label "a"}))
    (is (= "ns_cnt.my_label.a 1.0 1111\n"
           (to-graphite ""))))

  (testing "report registry with a counter and a gauge and labels"
    (metrics/clear-default-registry!)
    (metrics/register+execute :ns/cnt (p/counter {:labels [:my-label]}) (p/inc {:my-label 1}))
    (metrics/register+execute :ns/gauge (p/gauge {}) (p/inc {}))
    (is (= (set ["ns_gauge 1.0 1111"
                 "ns_cnt.my_label.1 1.0 1111"])
           (set (cs/split (to-graphite "") #"\n"))))))

