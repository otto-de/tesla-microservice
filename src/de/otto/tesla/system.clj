(ns de.otto.tesla.system
  (:require [com.stuartsierra.component :as c]
            [de.otto.tesla.stateful.serving :as serving]
            [de.otto.tesla.stateful.app-status :as app-status]
            [de.otto.tesla.stateful.configuring :as configuring]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.tesla.stateful.keep-alive :as keep-alive]
            [beckon :as beckon]
            [clojure.tools.logging :as log]
            [environ.core :as env :only [env]]
            [de.otto.tesla.stateful.routes :as routes]
            [clojure.data.json :as json :only [write-str]]))

(defn stop [system]
  (log/info "<- stopping system")
  (beckon/reinit-all!)
  (c/stop system)
  (log/info "<- system stopped"))

(defn start-system [system]
  (let [system (c/start system)] 
    (doseq [sig ["INT" "TERM"]]
      (reset! (beckon/signal-atom sig) #{(partial stop system)}))))

(defn empty-system [runtime-config]
  (c/system-map
    :keep-alive (keep-alive/new-keep-alive)
    :routes (routes/new-routes)
    :config (c/using (configuring/new-config runtime-config) [:keep-alive])
    :metering (c/using (metering/new-metering) [:config])
    :app-status (c/using (app-status/new-app-status) [:config :routes])
    :server (c/using (serving/new-server) [:config :routes])))

