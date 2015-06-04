(ns de.otto.tesla.stateful.serving
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :as jetty]
            [de.otto.tesla.stateful.handler :as handler]
            [clojure.tools.logging :as log]))

;; The serving component is the frontend of the system.
;; It accepts requests and returns the data to be used by consuming systems.
;; For the moment a simple, blocking implementation with an embedded jetty is chosen.
(defrecord Server [config handler]
  component/Lifecycle
  (start [self]
    (log/info "-> starting server")
    (let [port (Integer. (get-in config [:config :server-port]))
          all-routes (handler/handler handler)
          server (jetty/run-jetty all-routes {:port port :join? false})]
      (assoc self :server server)))

  (stop [self]
    (log/info "<- stopping server")
    (if-let [server (:server self)]
      (.stop server))
    self))

(defn new-server [] (map->Server {}))
