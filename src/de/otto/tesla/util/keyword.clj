(ns de.otto.tesla.util.keyword
  (:require [clojure.string :as str]))

;implementation is copied from environ 1.0.0
(defn- keywordize [s]
  (-> (str/lower-case s)
      (str/replace "_" "-")
      (str/replace "." "-")
      (keyword)))

(defn sanitize-keywords [m]
  (->> m
       (map (fn [[k v]] [(keywordize k) v]))
       (into {})))


;; Convert a map from string keys to keyword keys.
(defn keywordize-keys [m]
  (map (fn [[k v]] [(keyword k) v]) m))