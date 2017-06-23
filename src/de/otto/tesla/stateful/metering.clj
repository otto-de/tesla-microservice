(ns de.otto.tesla.stateful.metering
  (:require
    [compojure.core :as c]
    [com.stuartsierra.component :as component]
    [clojure.tools.logging :as log]
    [de.otto.tesla.metrics.core :as metrics]
    [de.otto.tesla.metrics.console :as metrics-console]
    [de.otto.tesla.metrics.graphite :as metrics-graphite]
    [de.otto.tesla.metrics.prometheus :as metrics-prometheus]
    [de.otto.tesla.stateful.handler :as handler]))

(defn- short-hostname [hostname]
  (re-find #"[^.]*" hostname))

(defn- start-reporter! [handler scheduler [reporter-type reporter-config]]
  (case reporter-type
    :console (metrics-console/start! reporter-config scheduler)
    :graphite (metrics-graphite/start! reporter-config scheduler)
    :prometheus (metrics-prometheus/register-endpoint! reporter-config handler)))

(defn metrics-response []
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (metrics/text-format)})

(defn- start-reporters! [config handler scheduler]
  (let [available-reporters (get-in config [:config :metrics])]
    (run! (partial start-reporter! handler scheduler) available-reporters)))

(defn make-handler [{metrics-path :metrics-path}]
  (c/routes (c/GET metrics-path [] (metrics-response))))

(defn register-endpoint! [prometheus-config handler]
  (log/info "Register metrics prometheus endpoint")
  (handler/register-handler handler (make-handler prometheus-config)))

#_(defn metered-execution [component-name fn & fn-params]
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



;; Initialises a metrics-registry and a graphite reporter.
(defrecord Metering [config handler scheduler]
  component/Lifecycle
  (start [self]
    (log/info "-> starting metering.")
    (assoc self :reporters (start-reporters! config handler scheduler)))

  (stop [self]
    (log/info "<- stopping metering")
    ; TODO How to stop metrics prometheus/graphite)
    self))

(defn new-metering [] (map->Metering {}))
