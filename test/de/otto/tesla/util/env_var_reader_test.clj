(ns de.otto.tesla.util.env-var-reader-test
  (:require [de.otto.tesla.util.env_var_reader :as env-reader]
            [clojure.test :refer :all]
            [environ.core :as env]))

(deftest ^:unit should-map-keys-to-sanitized-keywords
  (with-redefs [env/env {"my-var-key" "my-var-value"}]
    (is (= "my-var-value"
           (env-reader/read-env-var ["my-var-key"])))))

(deftest ^:unit should-use-fallback-if-env-does-not-have-key
  (with-redefs [env/env {}]
    (is (= "default"
           (env-reader/read-env-var ["my-var-key" "default"])))))

(deftest ^:unit should-return-empty-if-no-fallback-and-key-not-in-env
  (with-redefs [env/env {}]
    (is (= ""
           (env-reader/read-env-var ["my-var-key"])))))