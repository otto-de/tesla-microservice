(ns de.otto.tesla.util.sanitize-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.util.sanitize :as san]))

(deftest ^:unit should-sanitize-passwords
         (is (= (san/sanitize {:somerandomstuff                        "not-so-secret"
                                      :somerandomstuff-passwd-somerandomstuff "secret"
                                      :somerandomstuff-pwd-somerandomstuff    "longersecret"})
                {:somerandomstuff                        "not-so-secret"
                 :somerandomstuff-passwd-somerandomstuff "***"
                 :somerandomstuff-pwd-somerandomstuff    "***"})))