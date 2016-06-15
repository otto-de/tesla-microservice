(ns de.otto.tesla.util.sanitize-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.util.sanitize :as san]))

(deftest ^:unit should-sanitize-passwords
  (is (= (san/hide-passwds {:somerandomstuff                    "not-so-secret"
                        :somerandomstuff-passwd-somerandomstuff "secret"
                        :somerandomstuff-pwd-somerandomstuff    "longersecret"
                        :nested                                 {:some-passwd "secret"}})
         {:somerandomstuff                        "not-so-secret"
          :somerandomstuff-passwd-somerandomstuff "***"
          :somerandomstuff-pwd-somerandomstuff    "***"
          :nested                                 {:some-passwd "***"}
          })))