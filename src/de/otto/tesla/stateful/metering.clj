(ns de.otto.tesla.stateful.metering
  (:require
    [com.stuartsierra.component :as component]
    [clojure.tools.logging :as log]
    [de.otto.goo.goo :as goo]
    [de.otto.tesla.reporter.console :as prom-console]
    [de.otto.tesla.reporter.graphite :as prom-graphite]
    [de.otto.tesla.reporter.prometheus :as prom-endpoint]
    [de.otto.tesla.reporter.graphite-dropwizard :as metrics-graphite-dropwizard]
    [metrics.core :as dw]))

(defn- start-reporter! [handler scheduler [reporter-type reporter-config]]
  (case reporter-type
    :console (prom-console/start! reporter-config scheduler)
    :graphite (prom-graphite/start! reporter-config scheduler)
    :prometheus (prom-endpoint/register-endpoint! reporter-config handler)
    :graphite-dropwizard (metrics-graphite-dropwizard/start! dw/default-registry reporter-config)))


(defn- start-reporters! [config handler scheduler]
  (let [available-reporters (get-in config [:config :metrics])]
    (run! (partial start-reporter! handler scheduler) available-reporters)))

;; Initialises a metrics-registry and a graphite reporter.
(defrecord Metering [config handler scheduler]
  component/Lifecycle
  (start [self]
    (log/info "-> starting metering.")
    (goo/register-counter! :metering/errors {:labels [:error :metric-name]})
    (assoc self :reporters (start-reporters! config handler scheduler)))

  (stop [self]
    (log/info "<- stopping metering")
    ; TODO How to stop metrics graphite)
    self))

(defn new-metering [] (map->Metering {}))
