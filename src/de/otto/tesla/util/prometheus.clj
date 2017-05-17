(ns de.otto.tesla.util.prometheus
  (:require [metrics.core :as metrics]
            [clojure.string :as s])
  (:import (com.codahale.metrics Snapshot Histogram Gauge Counter)))

(defn- type-line [name type]
  (format "# TYPE %s %s" name type))

(defn counter->text [name ^Counter counter]
  [(type-line name "counter")
   (format "%s %s" name (.getCount counter))])

(defn gauge->text [name ^Gauge gauge]
  [(type-line name "gauge")
   (format "%s %s" name (.getValue gauge))])

(defn histogram->text [name ^Histogram histogram]
  (let [^Snapshot snapshot (.getSnapshot histogram)]
    [(type-line name "histogram")
     (format "%s{quantile=0.01} %s" name (.getValue snapshot (double 0.01)))
     (format "%s{quantile=0.05} %s" name (.getValue snapshot (double 0.05)))
     (format "%s{quantile=0.5} %s" name (.getMedian snapshot))
     (format "%s{quantile=0.9} %s" name (.getValue snapshot (double 0.9)))
     (format "%s{quantile=0.99} %s" name (.get99thPercentile snapshot))
     (format "%s_sum %s" name (reduce (fn [agg val] (+ agg val)) 0 (.getValues snapshot)))
     (format "%s_count %s" name (.getCount histogram))]))

(defn transform-metrics [metrics transform-fn]
  (reduce (fn [agg [name metric]] (concat agg (transform-fn name metric)))
          []
          metrics))

(defn collect-metrics [registry]
  (->> [""]
       (concat (transform-metrics (metrics/counters registry) counter->text))
       (concat (transform-metrics (metrics/histograms registry) histogram->text))
       (concat (transform-metrics (metrics/gauges registry) gauge->text))
       (s/join "\n")))