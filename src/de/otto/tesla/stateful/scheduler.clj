(ns de.otto.tesla.stateful.scheduler
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [overtone.at-at :as at]))


(defn schedule
  "Calls function repeatedly every ms-period milliseconds."

  ([self function# ms-period]
   (schedule self function# ms-period false))
  ([{executor :executor} function# ms-period interspaced?]
   "Calls function repeatedly every ms-period milliseconds if interspaced? is false.
    Else calls function every ms-period milliseconds after the function returned."
   (if interspaced?
     (at/interspaced ms-period function# executor)
     (at/every ms-period function# executor))))

(defn only-specified [config]
  (filter (fn [[_ v]] (not (nil? v))) config))

(defn as-seq [v]
  (apply concat v))

(defn new-pool [config]
  (let [config {:cpu-count (get-in config [:config :scheduler-num-threads])}]
    (apply at/mk-pool (-> (only-specified config) (as-seq)))))

(defrecord Scheduler [config]
  c/Lifecycle
  (start [self]
    (log/info "-> Start Scheduler")
    (let [new-self (assoc self
                     :schedules (atom [])
                     :executor (new-pool config))]
      new-self))

  (stop [self]
    (log/info "<- Stop Scheduler")
    (doseq [job @(:schedules self)]
      (at/kill job))
    (at/stop-and-reset-pool! (:executor self))
    self))

(defn new-scheduler []
  (map->Scheduler {}))
