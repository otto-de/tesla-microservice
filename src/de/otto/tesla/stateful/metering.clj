(ns de.otto.tesla.stateful.metering
  (:require
    [compojure.core :as c]
    [com.stuartsierra.component :as component]
    [clojure.tools.logging :as log]
    [de.otto.tesla.metrics.prometheus.core :as prom]
    [de.otto.tesla.metrics.prometheus.console :as prom-console]
    [de.otto.tesla.metrics.prometheus.graphite :as prom-graphite]
    [de.otto.tesla.metrics.prometheus.endpoint :as prom-endpoint]
    [de.otto.tesla.metrics.graphite-dropwizard :as metrics-graphite-dropwizard]
    [de.otto.tesla.stateful.handler :as handler]
    [metrics.core :as dw]
    [iapetos.core :as p]))

(defn- start-reporter! [handler scheduler [reporter-type reporter-config]]
  (case reporter-type
    :console (prom-console/start! reporter-config scheduler)
    :graphite (prom-graphite/start! reporter-config scheduler)
    :prometheus (prom-endpoint/register-endpoint! reporter-config handler)
    :graphite-dropwizard (metrics-graphite-dropwizard/start! dw/default-registry reporter-config)))


(defn- start-reporters! [config handler scheduler]
  (let [available-reporters (get-in config [:config :metrics])]
    (run! (partial start-reporter! handler scheduler) available-reporters)))

(defn monitor-transducer [metric-name fn & fn-params]
  (let [counter-name (keyword (str metric-name "/counter"))]
    (prom/register! (p/counter counter-name {:labels [:error]}))
    (try
      (let [return-value (apply fn fn-params)]
        (prom/register+execute! counter-name (p/counter :labels [:error]) (p/inc {:error false}))
        return-value)
      (catch Exception e
        (prom/inc! counter-name {:error true})
        (log/error e (str "Exception in " metric-name))
        (throw e)))))


;; Initialises a metrics-registry and a graphite reporter.
(defrecord Metering [config handler scheduler]
  component/Lifecycle
  (start [self]
    (log/info "-> starting metering.")
    (assoc self :reporters (start-reporters! config handler scheduler)))

  (stop [self]
    (log/info "<- stopping metering")
    ; TODO How to stop metrics graphite)
    self))

(defn new-metering [] (map->Metering {}))
