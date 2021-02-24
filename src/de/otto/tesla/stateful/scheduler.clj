(ns de.otto.tesla.stateful.scheduler
  "This components maintains a thread pool which can be used to schedule activities."
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [overtone.at-at :as ot]
            [de.otto.tesla.stateful.app-status :as app-status])
  (:import (java.util.concurrent ScheduledThreadPoolExecutor)))

(defn- as-seq [v]
  (apply concat v))

(defn- new-ot-pool [config]
  (let [pool-config (get-in config [:config :scheduler])]
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
      {:active    (.getActiveCount thread-pool)
       :queueSize (.size (.getQueue thread-pool))
       :poolSize  (.getPoolSize thread-pool)})))

(defn- scheduler-app-status [{:keys [pool]}]
  {:scheduler {:status        :ok
               :poolInfo      (pool-details pool)
               :scheduledJobs (into {} (map job-details (ot/scheduled-jobs pool)))}})

(defn exception-to-log [desc f]
  (try (f)
       (catch Exception e
         (log/error e (str "Exception during scheduled job: " desc)))))

(defn every
  "Calls fun every ms-period, and takes an optional initial-delay for
  the first call in ms.  Returns a scheduled-fn which may be cancelled
  with cancel. All exceptions are catched and logged.

  Default options are
  {:initial-delay 0 :desc \"\"}"

  [{:keys [pool]} ms-period fun & {:keys [initial-delay desc]
                                   :or   {initial-delay 0
                                          desc          ""}}]
  (ot/every ms-period (partial exception-to-log desc fun) pool :initial-delay initial-delay :desc desc))

(defn interspaced
  "Calls fun repeatedly with an interspacing of ms-period, i.e. the next
   call of fun will happen ms-period milliseconds after the completion
   of the previous call. Also takes an optional initial-delay for the
   first call in ms. Returns a scheduled-fn which may be cancelled with
   cancel. All exceptions are catched and logged.

   Default options are
   {:initial-delay 0 :desc \"\"}"
  [{:keys [pool]} ms-period fun & {:keys [initial-delay desc]
                                                         :or   {initial-delay 0
                                                                desc          ""}}]
  (ot/interspaced ms-period (partial exception-to-log desc fun) pool :initial-delay initial-delay :desc desc))


(defrecord Scheduler [config app-status]
  c/Lifecycle
  (start [self]
    (log/info "-> Start Scheduler")
    (let [new-self (assoc self :pool (new-ot-pool config))]
      (app-status/register-status-fun app-status (partial scheduler-app-status new-self))
      new-self))

  (stop [self]
    (log/info "<- Stop Scheduler")
    (when-let [pool (:pool self)]
      (ot/stop-and-reset-pool! pool))
    self))

(defn new-scheduler []
  (map->Scheduler {}))
