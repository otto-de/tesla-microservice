(ns de.otto.tesla.stateful.scheduler
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [overtone.at-at :as ot]
            [de.otto.tesla.stateful.app-status :as app-status])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor)))

(defn- as-seq [v]
  (apply concat v))

(defn- new-ot-pool [config]
  (let [pool-config (get-in config [:config :scheduler] {})]
    (apply ot/mk-pool (as-seq pool-config))))

(defn- as-readable-time [l]
  (.format (java.text.SimpleDateFormat. "EEEE HH':'mm':'ss's'") l))

(defn- job-details [{:keys [id created-at initial-delay desc ms-period scheduled?]}]
  [(keyword (str id)) {:desc         desc
                       :createdAt    (as-readable-time created-at)
                       :initialDelay initial-delay
                       :msPeriod     ms-period
                       :scheduled?   @scheduled?}])

(defn- pool-details [{:keys [pool-atom]}]
  (when pool-atom
    (let [^ScheduledThreadPoolExecutor thread-pool (:thread-pool @pool-atom)]
      {:threadPool {:active    (.getActiveCount thread-pool)
                    :queueSize (.size (.getQueue thread-pool))
                    :poolSize  (.getPoolSize thread-pool)}})))

(defn- scheduler-app-status [{:keys [pool]}]
  {:scheduler {:status        :ok
               :poolInfo      (pool-details pool)
               :scheduledJobs (into {} (map job-details (ot/scheduled-jobs pool)))}})

(defprotocol SchedulerPool
  (pool [self]))

(defrecord Scheduler [config app-status]
  c/Lifecycle
  (start [self]
    (log/info "-> Start Scheduler")
    (let [new-self (assoc self :pool (new-ot-pool config))]
      (app-status/register-status-fun app-status (partial scheduler-app-status new-self))
      new-self))

  (stop [self]
    (log/info "<- Stop Scheduler")
    (ot/stop-and-reset-pool! (:pool self))
    self)

  SchedulerPool
  (pool [self]
    (:pool self)))

(defn new-scheduler []
  (map->Scheduler {}))
