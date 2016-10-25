(ns de.otto.tesla.stateful.handler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [compojure.core :as c]))

(defprotocol HandlerContainer
  (register-handler [self handler] [self handler-name handler])
  (handler [self]))

(defn new-handler-name [self]
  (str "tesla-handler-" (count @(:the-handlers self))))

(defrecord Handler []
  component/Lifecycle
  (start [self]
    (log/info "-> starting Handler")
    (assoc self :the-handlers (atom [])))
  (stop [self]
    (log/info "<- stopping Handler")
    self)
  HandlerContainer
  (register-handler [self handler]
    (register-handler self (new-handler-name self) handler))

  (register-handler [self handler-name handler]
    (swap! (:the-handlers self) #(conj % [handler-name handler])))

  (handler [self]
    (let [handlers (:the-handlers self)]
      (apply c/routes (map second @handlers)))))

(defn new-handler []
  (map->Handler {}))
