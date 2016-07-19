(ns de.otto.tesla.stateful.scheduler
  (:require [com.stuartsierra.component :as c]
            [clojure.tools.logging :as log]
            [overtone.at-at :as at]))


(defn schedule [{executor :executor} function# ms-period]
  (at/every ms-period function# executor))


(defrecord Scheduler []
  c/Lifecycle
  (start [self]
    (log/info "-> Start Scheduler")
    (let [new-self (assoc self
                     :schedules (atom [])
                     :executor (at/mk-pool))]
      new-self))

  (stop [self]
    (log/info "<- Stop Scheduler")
    (doseq [job @(:schedules self)]
      (at/kill job))
    (at/stop-and-reset-pool! (:executor self))
    self))

(defn new-scheduler []
  (map->Scheduler {}))
