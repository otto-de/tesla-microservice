(ns de.otto.tesla.system
  (:require [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.app-status :as app-status]
            [de.otto.tesla.stateful.health :as health]
            [de.otto.goo.goo :as goo]
            [de.otto.tesla.stateful.configuring :as configuring]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.stateful.keep-alive :as keep-alive]
            [de.otto.tesla.stateful.scheduler :as scheduler]
            [beckon :as beckon]
            [clojure.tools.logging :as log]
            [environ.core :as env :only [env]]
            [de.otto.tesla.stateful.handler :as handler]
            [metrics.counters :as counters])
  (:import (clojure.lang ExceptionInfo)))

(defn wait! [system]
  (if-let [wait-time (get-in system [:config :config :wait-ms-on-stop])]
    (try
      (log/info "<- Waiting " wait-time " milliseconds.")
      (Thread/sleep (Integer. wait-time))
      (catch Exception e (log/error e)))))

(defn- exit [code]
  (System/exit code))

(defn- try-stop [system]
  (try
    (c/stop system)
    (log/info "System stopped.")
    (catch Exception ex
      (log/error ex "Error on stopping the system."))
    (finally (exit 1))))

(defn stop [system]
  (beckon/reinit-all!)
  (when-let [health (:health system)]
    (log/info "<- System will be stopped. Setting lock.")
    (health/lock-application health)
    (wait! system))
  (log/info "<- Stopping system.")
  (try-stop system))

(defn- try-start [system]
  (try
    (c/start system)
    (catch ExceptionInfo e
      (log/error (c/ex-without-components e) "Going to shut down because of this error.")
      (-> e (ex-data) :system (try-stop)))))

(defn start [system]
  (log/info "-> Starting system.")
  (let [started (try-start system)]
    (log/info "-> System completely started.")
    (goo/register-counter! :system-startups {:description "Counts startups."})
    (goo/inc! :system-startups)
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
