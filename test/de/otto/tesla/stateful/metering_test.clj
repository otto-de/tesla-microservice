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
            [metrics.core :as metrics]
            [metrics.timers :as timer]))

(def short-hostname #'metering/short-hostname)
(deftest short-hostname-test
  (testing "it only returns the important part of a full-qualified hostname"
    (is (= ""
           (short-hostname "")))
    (is (= "mesos-slave-dev-399946"
           (short-hostname "mesos-slave-dev-399946.lhotse.ov.otto.de")))))

#_(deftest metered-execution-test
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
