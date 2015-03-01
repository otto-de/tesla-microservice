(ns de.otto.tesla.system-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.system :as system]))

(deftest ^:unit should-start-empty-system-and-shut-it-down
  (u/with-started [started (system/empty-system {:server-port 56798})]
                  (is (= "look ma, no exceptions" "look ma, no exceptions"))))
