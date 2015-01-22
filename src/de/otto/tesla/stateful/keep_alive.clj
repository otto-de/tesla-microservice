(ns de.otto.tesla.stateful.keep-alive
  "This component is responsible for keeping the system alive."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defn do-nothing []
  (while true (Thread/sleep 60000)))

(defn keep-alive []
  (let [thread (Thread. do-nothing)]
    (.start thread)
    thread))

(defrecord KeepAlive []
  component/Lifecycle
  (start [self]
    (log/info "-> starting keepalive thread.")
    (assoc self :thread (keep-alive)))

  (stop [self]
    (log/info "<- stopping keepalive thread.")
    (.stop (:thread self))
    self))

(defn new-keep-alive [] (map->KeepAlive {}))
