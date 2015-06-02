(ns de.otto.tesla.stateful.keep-alive
  "This component is responsible for keeping the system alive by creating a non-deamonized noop thread."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defn do-nothing [running?]
  (while @running?
    (try
      (Thread/sleep 60000)
      (catch InterruptedException i
        (log/debug "keepalive thread sleep interrupted!"))
      (catch Exception e (throw e)))))

(defn keep-alive [running?]
  (let [thread (Thread. (partial do-nothing running?) "keepalive")]
    (.start thread)
    thread))

(defrecord KeepAlive []
  component/Lifecycle
  (start [self]
    (log/info "-> starting keepalive thread.")
    (let [running? (atom true)]
      (assoc self
        :running? running?
        :thread (keep-alive running?))))

  (stop [self]
    (log/info "<- stopping keepalive thread.")
    (try
      (when-let [running? (:running? self)]
        (reset! running? false)
        (.interrupt (:thread self)))                        ;; try the "not-so-unfriendly"-method first.
      (Thread/sleep 1000)
      (.stop (:thread self))                                ;; and finally kill it.
      (catch Exception e
        (log/debug e "Error stopping keepalive thread.")))
    self))

(defn new-keep-alive [] (map->KeepAlive {}))
