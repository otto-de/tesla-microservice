(ns de.otto.tesla.reporter.graphite
  (:require [de.otto.tesla.stateful.scheduler :as sched]
            [de.otto.goo.goo :as goo]
            [overtone.at-at :as at]
            [clojure.tools.logging :as log]
            [environ.core :as env]
            [clojure.string :as cs])
  (:import (java.net Socket)
           (java.io BufferedWriter PrintWriter Writer Closeable IOException)))

(defn- now-in-s []
  (quot (System/currentTimeMillis) 1000))

(defn- write-metrics! [write-fn! prefix]
  (let [sample (goo/serialize-metrics (now-in-s) prefix (goo/snapshot))]
    (run! write-fn! (flatten sample))))

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
      (goo/inc! :metrics/error {:type "graphite"}))))

(defn- push-to-graphite [{:keys [^String host port] :as graphite-config}]
  (let [prefix (prefix graphite-config (hostname))
        ^Socket s (Socket. host (Integer/parseInt port))
        ^BufferedWriter writer (BufferedWriter. (PrintWriter. (.getOutputStream s)))
        write-fn #(.write writer ^String %)]
    (try
      (log/infof "Reporting to Graphite %s:%s with %s as prefix" host port prefix)
      (write-metrics! write-fn prefix)
      (catch Exception e
        (log/error e "Error while reporting to Graphite.")
        (goo/inc! :metrics/error {:type "graphite"}))
      (finally
        (close writer)
        (close s)))))

(defn start! [graphite-config scheduler]
  (goo/register-counter! :metrics/error {:labels [:type]})
  (let [interval-in-ms (* 1000 (:interval-in-s graphite-config))]
    (log/info "Starting metrics Graphite reporter")
    (at/every interval-in-ms #(push-to-graphite graphite-config) (sched/pool scheduler) :desc "Metrics Graphite-Reporter")))

