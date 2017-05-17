(ns de.otto.tesla.stateful.metering
  (:require
    [com.stuartsierra.component :as component]
    [metrics.core :as metrics]
    [metrics.timers :as timers]
    [metrics.meters :as meters]
    [metrics.counters :as counters]
    [metrics.gauges :as gauges]
    [metrics.histograms :as histograms]
    [de.otto.tesla.util.prometheus :as prom]
    [metrics.reporters.graphite :as graphite]
    [metrics.reporters.console :as console]
    [clojure.tools.logging :as log]
    [de.otto.tesla.stateful.configuring :as configuring]
    [de.otto.tesla.stateful.handler :as handler]
    [compojure.core :as c])
  (:import
    (com.codahale.metrics MetricFilter Timer)
    (java.util.concurrent TimeUnit)))

(defn- short-hostname [hostname]
  (re-find #"[^.]*" hostname))

(defn- graphite-host-prefix [{:keys [config] :as c}]
  (let [external-hostname (configuring/external-hostname c)
        hostname (if (:graphite-shorten-hostname? config)
                   (short-hostname external-hostname)
                   external-hostname)]
    (str (:graphite-prefix config) "." hostname)))

(defn graphite-conf [{:keys [config] :as c}]
  {:host          (:graphite-host config)
   :port          (Integer. (:graphite-port config))
   :prefix        (graphite-host-prefix c)
   :rate-unit     TimeUnit/SECONDS
   :duration-unit TimeUnit/MILLISECONDS
   :filter        MetricFilter/ALL})

(defn- start-graphite! [registry {:keys [config] :as c}]
  (let [graphite-conf (graphite-conf c)
        reporter (graphite/reporter registry graphite-conf)]
    (log/info "-> starting graphite reporter:" graphite-conf)
    (graphite/start reporter (Integer/parseInt (:graphite-interval-seconds config)))
    reporter))

(defn- start-console! [registry {:keys [config]}]
  (let [reporter (console/reporter registry {})]
    (log/info "-> starting console reporter.")
    (console/start reporter (Integer/parseInt (:console-interval-seconds config)))
    reporter))

(defn- start-reporter! [registry config]
  (case (get-in config [:config :metering-reporter])
    "graphite" (start-graphite! registry config)
    "console" (start-console! registry config)
    nil                                                     ;; default: do nothing!
    ))

(defn update-timer! [^Timer timer timestamp-in-ms]
  (.update timer
           (- (System/currentTimeMillis) timestamp-in-ms)
           (TimeUnit/MILLISECONDS)))

(defn metered-execution [component-name fn & fn-params]
  (let [timing (timers/start (timers/timer [component-name "time"]))
        exception-meter (meters/meter [component-name "exception"])
        messages-meter (meters/meter [component-name "messages" "processed"])]
    (try
      (let [return-value (apply fn fn-params)]
        (meters/mark! messages-meter)
        return-value)
      (catch Exception e
        (meters/mark! exception-meter)
        (log/error e (str "Exception in " component-name))
        (throw e))
      (finally
        (timers/stop timing)))))

(defprotocol PubMetering
  (gauge! [self gauge-callback-fn name])
  (timer! [self name])
  (counter! [self name])
  (histogram! [self name]))


(defn metrics-response [self]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    (prom/collect-metrics (:registry self))})

(defn make-handler [self]
  (c/routes (c/GET "/metrics" [] (metrics-response self))))

;; Initialises a metrics-registry and a graphite reporter.
(defrecord Metering [config handler]
  component/Lifecycle
  (start [self]
    (log/info "-> starting metering.")
    (let [registry metrics/default-registry
          new-self (assoc self
                     :registry registry
                     :reporter (start-reporter! registry config))]
      (handler/register-handler handler (make-handler new-self))
      new-self))
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
    (counters/counter (:registry self) [name]))
  (histogram! [self name]
    (histograms/histogram (:registry self) [name])))


(defn new-metering [] (map->Metering {}))
