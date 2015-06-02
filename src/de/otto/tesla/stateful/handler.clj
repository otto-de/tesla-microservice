(ns de.otto.tesla.stateful.handler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [compojure.core :as c]))

(defprotocol HandlerContainer
  (register-handler [self routes])
  (handler [self]))

(defrecord Handler []
  component/Lifecycle
  (start [self]
    (log/info "-> starting Handler")
    (assoc self :the-handlers (atom [])))
  (stop [self]
    (log/info "<- stopping Handler")
    self)
  HandlerContainer
  (register-handler [self handler] (swap! (:the-handlers self) #(conj % handler)))
  (handler [self]
    (let [handlers (:the-handlers self)]
      (apply c/routes @handlers))))

(defn new-handler []
  (map->Handler {}))
