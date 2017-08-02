(ns de.otto.tesla.reporter.graphite-test
  (:require
    [clojure.test :refer :all]
    [de.otto.tesla.reporter.graphite :as graphite]
    [de.otto.goo.goo :as goo]
    [clojure.string :as cs]))

(defn to-graphite [prefix]
  (let [writer (atom "")]
    (with-redefs [graphite/now-in-s (constantly 1111)]
      (#'graphite/write-metrics! #(swap! writer str %) prefix)
      @writer)))

(deftest report-to-buffer-test
  (testing "report empty registry"
    (goo/clear-default-registry!)
    (is (= "" (to-graphite ""))))

  (testing "report registry with a counter"
    (goo/clear-default-registry!)
    (goo/register-counter! :ns/cnt {})
    (goo/inc! :ns/cnt)
    (is (= "ns_cnt 1.0 1111\n"
           (to-graphite ""))))

  (testing "report registry with a counter with a prefix"
    (goo/clear-default-registry!)
    (goo/register-counter! :ns/cnt {:labels [:my-label]})
    (goo/inc! :ns/cnt{:my-label "a"})
    (is (= "ns_cnt.my_label.a 1.0 1111\n"
           (to-graphite ""))))

  (testing "report registry with a counter and a gauge and labels"
    (goo/clear-default-registry!)
    (goo/register-counter! :ns/cnt {:labels [:my-label]})
    (goo/register-gauge! :ns/gauge {} 0)
    (goo/inc! :ns/cnt {:my-label 1})
    (goo/inc! :ns/gauge)
    (is (= (set ["ns_gauge 1.0 1111"
                 "ns_cnt.my_label.1 1.0 1111"])
           (set (cs/split (to-graphite "") #"\n"))))))

