(ns de.otto.tesla.stateful.metering-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.configuring :as configuring]
            [de.otto.tesla.metrics.core :as metrics]
            [iapetos.core :as p]))

(def short-hostname #'metering/short-hostname)
(deftest short-hostname-test
  (testing "it only returns the important part of a full-qualified hostname"
    (is (= ""
           (short-hostname "")))
    (is (= "mesos-slave-dev-399946"
           (short-hostname "mesos-slave-dev-399946.lhotse.ov.otto.de")))))

(deftest metered-execution-test
  (metrics/clear-default-registry!)
  (testing "it increments the call counter"
    (metering/monitor-transducer "prefix" (constantly nil) :a :b :c)
    (is (= 1.0
          (.get ((metrics/snapshot) :prefix/counter {:error false})))))
  (testing "it increments the call counter as error"
    (is (thrown? IllegalStateException (metering/monitor-transducer "prefix" (fn [& _args] (throw (IllegalStateException. "Noooo"))) :a :b :c)))
    (is (= 1.0
          (.get ((metrics/snapshot) :prefix/counter {:error true})))))
  )
