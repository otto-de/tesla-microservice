(ns de.otto.tesla.stateful.metering-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]
            [de.otto.tesla.stateful.configuring :as configuring]
            [de.otto.goo.goo :as goo]
            [iapetos.core :as p]))

(deftest metered-execution-test
  (goo/clear-default-registry!)
  (testing "it increments the call counter"
    (metering/monitor-transducer "prefix" (constantly nil) :a :b :c)
    (is (= 1.0
          (.get ((goo/snapshot) :prefix/counter {:error false})))))
  (testing "it increments the call counter as error"
    (is (thrown? IllegalStateException (metering/monitor-transducer "prefix" (fn [& _args] (throw (IllegalStateException. "Noooo"))) :a :b :c)))
    (is (= 1.0
          (.get ((goo/snapshot) :prefix/counter {:error true})))))
  )
