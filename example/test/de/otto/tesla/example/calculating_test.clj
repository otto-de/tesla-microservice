(ns de.otto.tesla.example.calculating-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.example.calculating :as calculating]
            [de.otto.tesla.example.example-system :as example-system]
            [de.otto.tesla.util.test-utils :as u]))
(deftest ^:unit calculations-should-be-counted
  (u/with-started [started (example-system/example-system {})]
                  (let [calculator (:calculator started)
                        result1 (calculating/calculate! calculator "foo")
                        result2 (calculating/calculate! calculator "bar")]
                    (is (= result1 "FOO"))
                    (is (= result2 "BAR"))
                    (is (= (calculating/calculations calculator) 2)))))