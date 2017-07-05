(ns de.otto.tesla.metrics.prometheus.core
  (:require [iapetos.core :as p]
            [iapetos.export :as e]
            [clojure.tools.logging :as log])
  (:import (io.prometheus.client SimpleCollector Collector)))

(def empty-registry (p/collector-registry))

(def default-registry (atom empty-registry))

(defn snapshot []
  @default-registry)

(defn clear-default-registry! []
  (.clear (.raw (snapshot)))                         ; for unknown reasons there is still state left in the underlying CollectorRegistry
  (reset! default-registry (p/collector-registry)))

(defn register! [& ms]
  (try
    (swap! default-registry (fn [r] (apply p/register r ms)))
    (catch IllegalArgumentException e
      (log/warn (.getMessage e)))))

(defmacro with-default-registry [& ops]
  `(-> (snapshot) ~@ops))

;TODO:
;(defn get-metric-value [name]
;  (.get ^Collector ((snapshot)) name))

(defmacro inc! [& opts]
  `(with-default-registry (p/inc ~@opts)))

(defmacro dec! [& opts]
  `(with-default-registry (p/dec ~@opts)))

(defmacro set! [& opts]
  `(with-default-registry (p/set ~@opts)))

(defmacro observe! [& opts]
  `(with-default-registry (p/observe ~@opts)))

(defmacro register+execute! [name m op]
  `(do
     (when-not ((snapshot) ~name)
       (register! (~(first m) ~name ~@(rest m))))
     (~(first op) (snapshot) ~name ~@(rest op))))

(defn get-from-default-registry
  ([name]
   (get-from-default-registry name {}))
  ([name labels]
   ((snapshot) name labels)))

(defn text-format []
  (e/text-format (snapshot)))

