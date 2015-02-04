(ns de.otto.tesla.stateful.metering
  (:require
    [com.stuartsierra.component :as component]
    [metrics.core :as metrics]
    [metrics.timers :as timers]
    [metrics.counters :as counters]
    [metrics.gauges :as gauges]
    [metrics.reporters.graphite :as graphite]
    [metrics.reporters.console :as console]
    [clojure.tools.logging :as log])
  (:import
    (com.codahale.metrics MetricFilter)
    (java.util.concurrent TimeUnit)
    (java.net InetAddress UnknownHostException)))


(defn hostname-from-os []
  (.getCanonicalHostName (InetAddress/getLocalHost)))

(defn hostname []
  (try
    (hostname-from-os)
    (catch UnknownHostException e
      (log/error e " exception while trying to determine hostname")
      "default")))

(defn prefix [config]
  (str (:graphite-prefix config) "." (hostname)))

(defn start-graphite! [registry config]
  (let [reporter (graphite/reporter registry
                                    {:host          (:graphite-host config)
                                     :port          (Integer. (:graphite-port config))
                                     :prefix        (prefix config)
                                     :rate-unit     TimeUnit/SECONDS
                                     :duration-unit TimeUnit/MILLISECONDS
                                     :filter        MetricFilter/ALL})]
    (log/info "-> starting graphite reporter.")
    (graphite/start reporter (Integer/parseInt (:graphite-interval-seconds config)))
    reporter))

(defn start-console! [registry config]
  (let [reporter (console/reporter registry {})]
    (log/info "-> starting console reporter.")
    (console/start reporter (Integer/parseInt (:console-interval-seconds config)))
    reporter))

;; Configures a graphite reporter as configured.
(defn start-reporter! [registry config]
  (let [reporter-type (:metering-reporter config)]
    (case reporter-type
      "graphite" (start-graphite! registry config)
      "console" (start-console! registry config))))

;; Initialises a metrics-registry and a graphite reporter.
(defrecord Metering [config]
  component/Lifecycle
  (start [self]
    (log/info "-> starting metering.")
    (let [config (:config (:config self))
          registry (metrics/new-registry)
          reporter (start-reporter! registry config)]
      (assoc self
             :registry registry
             :reporter reporter)))
  (stop [self]
    (log/info "<- stopping metering")
    (.stop (:reporter self))
    self))

;; creates a timer. That can be kept and used later.
(defn timer! [self name]
  (timers/timer (:registry self) name))

;; dito
(defn counter! [self name]
  (counters/counter (:registry self) name))

;; dito
(defn gauge! [self gauge-callback-fn name]
     (gauges/gauge-fn (:registry self) name gauge-callback-fn))


(defn new-metering [] (map->Metering {}))
