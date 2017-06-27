(ns de.otto.tesla.metrics.prometheus.core-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.metrics.prometheus.core :as metrics]
            [iapetos.core :as p]
            [clojure.tools.logging :as log]
            [clojure.string :as str])
  (:import (iapetos.registry IapetosRegistry)))

(use-fixtures :each #(do (metrics/clear-default-registry!) (%)))

(deftest register-test
  (testing "it registers a metric under a name"
    (metrics/register (p/counter :app/requests))
    (is ((metrics/snapshot) :app/requests)))

  (testing "it warns if the metric is already registered"
    (metrics/clear-default-registry!)
    (let [logged (atom [])]
      (with-redefs [log/log* (fn [_ level _ message] (swap! logged conj [level message]))]
        (metrics/register (p/counter :app_requests))
        (metrics/register (p/counter :app_requests))
        (let [warning (first @logged)]
          (is (= 1 (count @logged)))
          (is (= (first warning) :warn))
          (is (str/includes? (second warning) "app_requests")))))))

(deftest with-default-registry-test
  (testing "it uses the default registry for lookup of metrics"
    (metrics/register (p/counter :app/requests))
    (metrics/with-default-registry (p/inc :app/requests))
    (metrics/with-default-registry (p/inc :app/requests))
    (is (= 2.0
           (.get ((metrics/snapshot) :app/requests))))))

(defn- samples [registry]
  (->> registry
       (.metricFamilySamples)
       (enumeration-seq)
       (map #(vec (.-samples %)))
       (flatten)))

(deftest register+execute-test
  (testing "it allows to register metrics with the default registry on the fly"
    (metrics/register+execute :app/requests (p/counter {}) (p/inc {}))
    (metrics/register+execute :app/requests (p/counter {}) (p/inc {}))
    (is (= 2.0
           (.get ((metrics/snapshot) :app/requests)))))

  (testing "it allows to register metrics with the default registry with labels on the fly"
    (metrics/register+execute :app/requests2 (p/counter {:labels [:rc]}) (p/inc {:rc 200}))
    (metrics/register+execute :app/requests2 (p/counter {}) (p/inc {:rc 500}))
    (is (= 1.0
           (.get ((metrics/snapshot) :app/requests2 {:rc 200}))
           (.get ((metrics/snapshot) :app/requests2 {:rc 500}))))))

(deftest inc-test
  (testing "it calls the iapetos inc function with metric name"
    (let [called-with (atom nil)]
      (with-redefs [p/inc (fn [reg metric] (swap! called-with (constantly metric)))]
        (metrics/register (p/counter :cntr1))
        (metrics/inc :cntr1)
        (is (= :cntr1
               @called-with)))))

  (testing "it calls the iapetos inc function with metric name and labels"
    (let [called-with (atom nil)]
      (with-redefs [p/inc (fn [reg metric lb] (swap! called-with (constantly [metric lb])))]
        (metrics/register (p/counter :cntr2 {:labels [:a]}))
        (metrics/inc :cntr2 {:a "a"})
        (is (= [:cntr2 {:a "a"}]
               @called-with))))))

(deftest dec-test
  (testing "it calls the iapetos dec function with metric name"
    (let [called-with (atom nil)]
      (with-redefs [p/dec (fn [reg metric] (swap! called-with (constantly metric)))]
        (metrics/register (p/gauge :gauge1))
        (metrics/dec :gauge1)
        (is (= :gauge1
               @called-with)))))

  (testing "it calls the iapetos dec function with metric name and labels"
    (let [called-with (atom nil)]
      (with-redefs [p/dec (fn [reg metric lb] (swap! called-with (constantly [metric lb])))]
        (metrics/register (p/gauge :gauge2 {:labels [:a]}))
        (metrics/dec :gauge2 {:a "a"})
        (is (= [:gauge2 {:a "a"}]
               @called-with))))))

(deftest set-macro-test
  (testing "it calls the iapetos set function with metric name"
    (let [called-with (atom nil)]
      (with-redefs [p/set (fn [reg metric amount] (swap! called-with (constantly [metric amount])))]
        (metrics/register (p/gauge :gauge1))
        (metrics/set :gauge1 5)
        (is (= [:gauge1 5]
               @called-with)))))

  (testing "it calls the iapetos set function with metric name and labels"
    (let [called-with (atom nil)]
      (with-redefs [p/set (fn [reg metric lb amount] (swap! called-with (constantly [metric lb amount])))]
        (metrics/register (p/gauge :gauge2 {:labels [:a]}))
        (metrics/set :gauge2 {:a "a"} 5)
        (is (= [:gauge2 {:a "a"} 5]
               @called-with))))))
