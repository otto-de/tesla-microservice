(ns de.otto.tesla.util.env_var_reader
  (:require [environ.core :as env]))

(defn read-env-var [env-var-key]
  (or
    (get env/env env-var-key)
    (throw (RuntimeException. (str "An error occured when trying to read property " env-var-key " from env")))))