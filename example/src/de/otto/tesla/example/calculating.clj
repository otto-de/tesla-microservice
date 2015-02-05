(ns de.otto.tesla.example.calculating
  (:require [com.stuartsierra.component :as component]
            [de.otto.tesla.stateful.metering :as metering]
            [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.app-status :as app-status]
            [de.otto.status :as s]
            [metrics.timers :as timers]))

;; status turns warning after 10 calculations. Because license expired.
(defn- status-fun
  [calculations]
  (if (> 10 @calculations)
    (s/status-detail :calculator :ok "less than 10 calculations performed")
    (s/status-detail :calculator :warning "more than 10 calculations perormed. Renew license.")))

(defprotocol PubCalculator
  (calculations [self])
  (calculate! [self input]))

;; The Calculator-Component is a example implementation for demonstration purposes
;; right now. It is intended to perform expensive Calculations and can be used
;; with or without the caching Component.
;; It is initialised with a calculation function and it maintains
;; the total number of calculations it has performed.
(defrecord Calculator [fun]
  component/Lifecycle
  (start [self]
    (log/info "-> starting example calculator.")
    (let [new-self
          (assoc self
            :timer (metering/timer! (:metering self) "calculations")
            :calculations (atom 0)
            :fun fun)]
      (app-status/register-status-fun (:app-status new-self)
                                      (partial status-fun (:calculations new-self)))
      new-self))

  (stop [self]
    (log/info "<- stopping example calculator.")
    (reset! (:calculations self) 0)
    self)
  PubCalculator
  (calculations [self] @(:calculations self))
  (calculate! [self input]
    (timers/time! (:timer self)
                  (swap! (:calculations self) inc)
                  ((:fun self) input))))

(defn new-calculator [fun] (map->Calculator {:fun fun}))
