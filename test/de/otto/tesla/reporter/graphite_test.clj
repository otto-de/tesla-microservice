(ns de.otto.tesla.reporter.graphite-test
  (:require
    [clojure.test :refer :all]
    [de.otto.tesla.reporter.graphite :as graphite]
    [de.otto.goo.goo :as goo]
    [iapetos.core :as p]
    [clojure.string :as cs]))

(defn to-graphite [prefix]
  (let [writer (atom "")]
    (with-redefs [graphite/now-in-s (constantly 1111)]
      (#'graphite/write-metrics! #(swap! writer str %) prefix (goo/snapshot))
      @writer)))

(deftest report-to-buffer-test
  (testing "report empty registry"
    (goo/clear-default-registry!)
    (is (= "" (to-graphite ""))))

  (testing "report registry with a counter"
    (goo/clear-default-registry!)
    (goo/register+execute! :ns/cnt (p/counter {}) (p/inc {}))
    (is (= "ns_cnt 1.0 1111\n"
           (to-graphite ""))))

  (testing "report registry with a counter with a prefix"
    (goo/clear-default-registry!)
    (goo/register+execute! :ns/cnt (p/counter {:labels [:my-label]}) (p/inc {:my-label "a"}))
    (is (= "ns_cnt.my_label.a 1.0 1111\n"
           (to-graphite ""))))

  (testing "report registry with a counter and a gauge and labels"
    (goo/clear-default-registry!)
    (goo/register+execute! :ns/cnt (p/counter {:labels [:my-label]}) (p/inc {:my-label 1}))
    (goo/register+execute! :ns/gauge (p/gauge {}) (p/inc {}))
    (is (= (set ["ns_gauge 1.0 1111"
                 "ns_cnt.my_label.1 1.0 1111"])
           (set (cs/split (to-graphite "") #"\n"))))))

