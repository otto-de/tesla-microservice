(ns de.otto.tesla.metrics.prometheus.graphite
  (:require [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.scheduler :as sched]
            [de.otto.tesla.metrics.prometheus.core :as metrics]
            [overtone.at-at :as at]
            [clojure.tools.logging :as log]
            [environ.core :as env]
            [clojure.string :as cs]
            [clj-time.core :as time]
            [clojure.string :as str]
            [iapetos.core :as p])
  (:import (io.prometheus.client CollectorRegistry Collector$MetricFamilySamples Collector$MetricFamilySamples$Sample)
           (java.net Socket)
           (java.io BufferedWriter PrintWriter Writer Closeable IOException)
           (iapetos.registry IapetosRegistry)))

(defn- ^String cleansed [^String s]
  (str/replace s #"[^a-zA-Z0-9_-]" "_"))

(defn- now-in-s []
  (quot (System/currentTimeMillis) 1000))

(defn- samples [registry]
  (->> registry
       (.metricFamilySamples)
       (enumeration-seq)
       (map #(vec (.-samples %)))
       (flatten)))

(defn- cleansed-labels [sample]
  (->> (interleave (.-labelNames sample) (.-labelValues sample))
       (map cleansed)
       (partition 2)))

(defn- write-metrics! [write-fn! prefix ^IapetosRegistry registry]
  (let [now-in-s (now-in-s)
        samples (samples (.raw registry))]
    (doseq [^Collector$MetricFamilySamples$Sample sample samples]
      (write-fn! prefix)
      (write-fn! (cleansed (.name sample)))
      (doseq [[name value] (cleansed-labels sample)]
        (write-fn! (format ".%s.%s" name value)))
      (write-fn! (format " %s %d\n" (.value sample) now-in-s)))))

(defn- short-hostname [hostname]
  (re-find #"[^.]*" hostname))

(defn- hostname []
  (or (:host env/env) (:host-name env/env) (:hostname env/env)
      "localhost"))

(defn- prefix [{:keys [prefix include-hostname]} app-hostname]
  (let [hostname-part (case include-hostname
                        :full (identity app-hostname)
                        :first-part (short-hostname app-hostname)
                        nil nil)
        p (cs/join "." (remove nil? [prefix hostname-part]))]
    (str p (when-not (empty? p) "."))))

(defn- close [^Closeable c]
  (try
    (.close c)
    (catch IOException e
      (log/error e "Could not close " c ".")
      (metrics/register+execute :metrics/error {:labels [:type]} (p/inc {:type "graphite"})))))

(defn- push-to-graphite [{:keys [^String host port] :as graphite-config}]
  (let [prefix (prefix graphite-config (hostname))
        ^Socket s (Socket. host (Integer/parseInt port))
        ^BufferedWriter writer (BufferedWriter. (PrintWriter. (.getOutputStream s)))
        write-fn #(.write writer ^String %)]
    (try
      (log/infof "Reporting to Graphite %s:%s with %s as prefix" host port prefix)
      (write-metrics! write-fn prefix (metrics/snapshot))
      (catch Exception e
        (log/error e "Error while reporting to Graphite.")
        (metrics/register+execute :metrics/error {:labels [:type]} (p/inc {:type "graphite"})))
      (finally
        (close writer)
        (close s)))))

(defn start! [graphite-config scheduler]
  (let [interval-in-ms (* 1000 (:interval-in-s graphite-config))]
    (log/info "Starting metrics Graphite reporter")
    (at/every interval-in-ms #(push-to-graphite graphite-config) (sched/pool scheduler) :desc "Metrics Graphite-Reporter")))

