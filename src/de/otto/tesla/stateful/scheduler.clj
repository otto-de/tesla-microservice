(ns de.otto.tesla.stateful.scheduler
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [overtone.at-at :as at]))

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
