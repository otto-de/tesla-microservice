(ns de.otto.tesla.metrics.prometheus.core
  (:require [iapetos.core :as p]
            [iapetos.export :as e]
            [clojure.tools.logging :as log]
            [iapetos.core :as prom]
            [clojure.string :as str])
  (:import (io.prometheus.client SimpleCollector Collector)))

(def empty-registry (p/collector-registry))

(def default-registry (atom empty-registry))

(defn snapshot []
  @default-registry)

(defn clear-default-registry! []
  (.clear (.raw (snapshot)))                                ; for unknown reasons there is still state left in the underlying CollectorRegistry
  (reset! default-registry (p/collector-registry)))

(defn get-from-default-registry
  ([name]
   (get-from-default-registry name {}))
  ([name labels]
   ((snapshot) name labels)))

(defmacro with-default-registry [& ops]
  `(-> (snapshot) ~@ops))

(defn- register-with-action [action ms]
  (try
    (swap! default-registry (fn [r] (apply p/register r ms)))
    (catch IllegalArgumentException e
      (action e))))

(defn register! [& ms]
  (register-with-action #(log/warn (.getMessage %)) ms))

(defn quiet-register! [& ms]
  (register-with-action (fn [_]) ms))

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

(defn text-format []
  (e/text-format (snapshot)))

(defn- compojure-path->url-path [cpj-path]
  (->> (str/split cpj-path #"/")
       (filter #(not (re-matches #"^:.*" %)))
       (filter #(not (re-matches #"^:.*" %)))
       (str/join "/")))

(defn timing-middleware [handler]
  (let [http-labels [:path :method :rc]]
    (quiet-register! (p/histogram :http/duration-in-s {:labels http-labels :buckets [0.05 0.1 0.15 0.2]}))
    (quiet-register! (p/counter :http/calls-total {:labels http-labels})))
  (fn [request]
    (if-let [response (handler request)]
      (let [[method path] (:compojure/route request)
            labels {:path   (compojure-path->url-path path)
                    :method method
                    :rc     (:status response)}]
        (inc! :http/calls-total labels)
        (prom/with-duration (get-from-default-registry :http/duration-in-s labels) response)))))


