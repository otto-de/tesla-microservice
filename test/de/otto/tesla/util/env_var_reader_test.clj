(ns de.otto.tesla.util.env-var-reader-test
  (:require [de.otto.tesla.util.env_var_reader :as env-reader]
            [clojure.test :refer :all]
            [environ.core :as env]))

(deftest ^:unit should-map-keys-to-sanitized-keywords
  (with-redefs-fn {#'env/env {"my-var-key" "my-var-value"}}
    #(is (= (env-reader/read-env-var "my-var-key") "my-var-value")))
  )