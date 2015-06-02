(ns de.otto.tesla.stateful.httpkit
  (:require [com.stuartsierra.component :as component]
            [org.httpkit.server :refer [run-server]]
            [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.handler :as handler])
  (:import [clojure.lang RT]))





(def default-port 3000)

;; The serving component is the frontend of the system.
;; It accepts requests and returns the data to be used by consuming systems.
;; For the moment a simple, blocking implementation with an embedded jetty is chosen.
(defrecord Server [config handler]
  component/Lifecycle
  (start [self]
    (log/info "-> starting server")
    (let [port (get-in config [:config :server :port] default-port)
          bind (get-in config [:config :server :bind] "0.0.0.0")
          handler (handler/handler handler)
          server (run-server handler
                             {:port port
                              :ip bind})]
      (assoc self :server server)))

  (stop [self]
    (log/info "<- stopping server")
    (when-let [server (:server self)]
      (server))
    self))

(defn new-server [] (map->Server {}))

