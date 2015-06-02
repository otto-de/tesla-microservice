(ns de.otto.tesla.stateful.metering
  (:require
    [com.stuartsierra.component :as component]
    [metrics.core :as metrics]
    [metrics.timers :as timers]
    [metrics.counters :as counters]
    [metrics.gauges :as gauges]
    [metrics.reporters.graphite :as graphite]
    [metrics.reporters.console :as console]
    [clojure.tools.logging :as log]
    [de.otto.tesla.stateful.configuring :as configuring])
  (:import
    (com.codahale.metrics MetricFilter)
    (java.util.concurrent TimeUnit)))

(defn prefix [config]
  (str (:graphite-prefix (:config config)) "." (configuring/external-hostname config)))

(defn- start-graphite! [registry config]
  (let [reporter (graphite/reporter registry
                                    {:host          (:graphite-host (:config config))
                                     :port          (Integer. (:graphite-port (:config config)))
                                     :prefix        (prefix config)
                                     :rate-unit     TimeUnit/SECONDS
                                     :duration-unit TimeUnit/MILLISECONDS
                                     :filter        MetricFilter/ALL})]
    (log/info "-> starting graphite reporter.")
    (graphite/start reporter (Integer/parseInt (:graphite-interval-seconds (:config config))))
    reporter))

(defn- start-console! [registry config]
  (let [reporter (console/reporter registry {})]
    (log/info "-> starting console reporter.")
    (console/start reporter (Integer/parseInt (:console-interval-seconds (:config config))))
    reporter))

(defn- start-reporter! [registry config]
  (case (:metering-reporter (:config config))
    "graphite" (start-graphite! registry config)
    "console" (start-console! registry config)
    nil ;; default: do nothing!
    ))

(defprotocol PubMetering
  (gauge! [self gauge-callback-fn name])
  (timer! [self name])
  (counter! [self name]))

;; Initialises a metrics-registry and a graphite reporter.
(defrecord Metering [config]
  component/Lifecycle
  (start [self]
    (log/info "-> starting metering.")
    (let [registry (metrics/new-registry)]
        (assoc self
               :registry registry
               :reporter (start-reporter! registry config))))
  (stop [self]
    (log/info "<- stopping metering")
    (when-let [reporter (:reporter self)]
      (.stop reporter))
    self)
  PubMetering
  (gauge! [self gauge-callback-fn name]
    (gauges/gauge-fn (:registry self) [name] gauge-callback-fn))
  (timer! [self name]
    (timers/timer (:registry self) [name]))
  (counter! [self name]
    (counters/counter (:registry self) [name])))


(defn new-metering [] (map->Metering {}))
