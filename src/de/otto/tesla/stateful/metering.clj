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

(defn- start-graphite! [registry config]
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

(defn- start-console! [registry config]
  (let [reporter (console/reporter registry {})]
    (log/info "-> starting console reporter.")
    (console/start reporter (Integer/parseInt (:console-interval-seconds config)))
    reporter))

(defn- start-reporter! [registry config]
  (case (:metering-reporter config)
    "graphite" (start-graphite! registry config)
    "console" (start-console! registry config)))

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
               :reporter (start-reporter! registry (get-in config [:config])))))
  (stop [self]
    (log/info "<- stopping metering")
    (.stop (:reporter self))
    self)
  PubMetering
  (gauge! [self gauge-callback-fn name]
    (gauges/gauge-fn (:registry self) [name] gauge-callback-fn))
  (timer! [self name]
    (timers/timer (:registry self) [name]))
  (counter! [self name]
    (counters/counter (:registry self) [name])))


(defn new-metering [] (map->Metering {}))
