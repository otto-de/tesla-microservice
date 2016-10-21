(ns de.otto.tesla.stateful.scheduler
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [de.otto.tesla.stateful.app-status :as app-status])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor)))

(defn as-seq [v]
  (apply concat v))

(defn schedule
  "Calls function repeatedly every ms-period milliseconds if interspaced? is false.
    Else calls function every ms-period milliseconds after the function returned."
  [{:keys [executor]} function# ms-period & {:as args}]
  (if (:interspaced? args)
    (apply (partial at/interspaced ms-period function# executor) (as-seq args))
    (apply (partial at/every ms-period function# executor) (as-seq args))))

(defn new-pool [config]
  (let [scheduler-config (get-in config [:config :scheduler] {})]
    (apply at/mk-pool (-> scheduler-config (as-seq)))))

(defn as-readable-time [l]
  (.format (java.text.SimpleDateFormat. "EEEE HH':'mm':'ss's'") l))

(defn job-details [{:keys [id created-at initial-delay desc ms-period scheduled?]}]
  [(keyword (str id)) {:desc         desc
                       :createdAt    (as-readable-time created-at)
                       :initialDelay initial-delay
                       :msPeriod     ms-period
                       :scheduled?   @scheduled?}])

(defn pool-details [{:keys [pool-atom]}]
  (when pool-atom
    (let [{:keys [^ScheduledThreadPoolExecutor thread-pool]} @pool-atom]
      {:threadPool {:active    (.getActiveCount thread-pool)
                    :queueSize (.size (.getQueue thread-pool))
                    :poolSize  (.getPoolSize thread-pool)}})))

(defn scheduler-app-status [{:keys [executor]}]
  {:scheduler {:status        :ok
               :poolInfo      (pool-details executor)
               :scheduledJobs (into {} (map job-details (at/scheduled-jobs executor)))}})

(defrecord Scheduler [config app-status]
  c/Lifecycle
  (start [self]
    (log/info "-> Start Scheduler")
    (let [new-self (assoc self
                     :schedules (atom [])
                     :executor (new-pool config))]
      (app-status/register-status-fun app-status (partial scheduler-app-status new-self))
      new-self))

  (stop [self]
    (log/info "<- Stop Scheduler")
    (doseq [job @(:schedules self)]
      (at/kill job))
    (at/stop-and-reset-pool! (:executor self))
    self))

(defn new-scheduler []
  (map->Scheduler {}))
