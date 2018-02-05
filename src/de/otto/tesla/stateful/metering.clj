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

(defn- start-reporter! [handler scheduler prometheus-auth-fn [reporter-type reporter-config]]
  (case reporter-type
    :console (prom-console/start! reporter-config scheduler)
    :graphite (prom-graphite/start! reporter-config scheduler)
    :prometheus (prom-endpoint/register-endpoint! reporter-config handler prometheus-auth-fn)
    :graphite-dropwizard (metrics-graphite-dropwizard/start! dw/default-registry reporter-config)))


(defn- start-reporters! [config handler scheduler prometheus-auth-fn]
  (let [available-reporters (get-in config [:config :metrics])]
    (run! (partial start-reporter! handler scheduler prometheus-auth-fn) available-reporters)))

;; Initialises a metrics-registry and a graphite reporter.
(defrecord Metering [prometheus-auth-fn config handler scheduler]
  component/Lifecycle
  (start [self]
    (log/info "-> starting metering.")
    (goo/register-counter! :metering/errors {:labels [:error :metric-name]})
    (assoc self :reporters (start-reporters! config handler scheduler prometheus-auth-fn)))

  (stop [self]
    (log/info "<- stopping metering")
    ; TODO How to stop metrics graphite)
    self))

(defn new-metering
  ([]
   (new-metering nil))
  ([prometheus-auth-fn]
   (map->Metering {:prometheus-auth-fn prometheus-auth-fn})))
