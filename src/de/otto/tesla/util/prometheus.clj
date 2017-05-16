(ns de.otto.tesla.util.prometheus
  (:import (com.codahale.metrics Snapshot Histogram Gauge Counter)))

(defn hist-to-prometheus [name ^Histogram histo-obj]
  (let [^Snapshot snapshot (.getSnapshot histo-obj)
        sum (reduce (fn [agg val] (+ agg val)) 0 (.getValues snapshot))]
    [(str "# TYPE " name " histogram")
     (str name "{quantile=0.01} " (.getValue snapshot (double 0.01)))
     (str name "{quantile=0.05} " (.getValue snapshot (double 0.05)))
     (str name "{quantile=0.5} " (.getMedian snapshot))
     (str name "{quantile=0.9} " (.getValue snapshot (double 0.9)))
     (str name "{quantile=0.99} " (.get99thPercentile snapshot))
     (str name "_sum " sum)
     (str name "_count " (.getCount histo-obj))]))

(defn hists-to-prometheus [histos]
  (let [hist->histvals (into {} (map (fn [[name histo-obj]] [name (hist-to-prometheus name histo-obj)])
                                     histos))]
    (reduce (fn [agg str-list] (concat agg str-list))
            []
            (vals hist->histvals))))

(defn count-to-prometheus [name ^Counter count]
  [(format "# TYPE %s counter" name)
   (str name " " (.getCount count))])

(defn counts-to-prometheus [counts]
  (let [name->countvals (into {} (map (fn [[name count]] [name (count-to-prometheus name count)])
                                      counts))]
    (reduce (fn [agg vals] (concat agg vals))
            []
            (vals name->countvals))))



(defn gauge-to-prometheus [name ^Gauge gauge]
  [(format "# TYPE %s gauge" name)
   (str name " " (.getValue gauge))])

(defn gauges-to-prometheus [gauges]
  (let [name->gaugevals (into {} (map (fn [[name gauge]] [name (gauge-to-prometheus name gauge)])
                                      gauges))]
    (reduce (fn [agg vals] (concat agg vals))
            []
            (vals name->gaugevals))))
