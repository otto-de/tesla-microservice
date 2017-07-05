(ns de.otto.tesla.system
  (:require [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.app-status :as app-status]
            [de.otto.tesla.stateful.health :as health]
            [de.otto.tesla.metrics.prometheus.core :as metrics]
            [iapetos.core :as prom]
            [de.otto.tesla.stateful.configuring :as configuring]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.stateful.keep-alive :as keep-alive]
            [de.otto.tesla.stateful.scheduler :as scheduler]
            [beckon :as beckon]
            [clojure.tools.logging :as log]
            [environ.core :as env :only [env]]
            [de.otto.tesla.stateful.handler :as handler]
            [metrics.counters :as counters]))

(defn wait! [system]
  (if-let [wait-time (get-in system [:config :config :wait-ms-on-stop])]
    (try
      (log/info "<- Waiting " wait-time " milliseconds.")
      (Thread/sleep (Integer. wait-time))
      (catch Exception e (log/error e)))))

(defn stop [system]
  (beckon/reinit-all!)
  (log/info "<- System will be stopped. Setting lock.")
  (health/lock-application (:health system))
  (wait! system)
  (log/info "<- Stopping system.")
  (c/stop system))

(defn start [system]
  (log/info "-> Starting system.")
  (let [started (c/start system)]
    (log/info "-> System completely started.")
    (metrics/register+execute! :system-startups (prom/counter {}) (prom/inc {}))
    (doseq [sig ["INT" "TERM"]]
      (reset! (beckon/signal-atom sig) #{(partial stop started)}))
    started))

(defn base-system [runtime-config]
  (c/system-map
    :keep-alive (keep-alive/new-keep-alive)
    :config (c/using (configuring/new-config runtime-config) [:keep-alive])
    :handler (c/using (handler/new-handler) [:config])
    :metering (c/using (metering/new-metering) [:config :handler :scheduler])
    :health (c/using (health/new-health) [:config :handler])
    :app-status (c/using (app-status/new-app-status) [:config :handler])
    :scheduler (c/using (scheduler/new-scheduler) [:config :app-status])))
