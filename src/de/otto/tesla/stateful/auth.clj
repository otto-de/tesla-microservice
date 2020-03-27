(ns de.otto.tesla.stateful.auth
  "This component handles authentication."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]))

(defn no-auth-middleware [_config handler-fn]
  (log/warn "You are using no authentication...Is this desired?")
  (fn [request]
    (handler-fn request)))

(defrecord AuthMiddleware [config auth-mw]
  component/Lifecycle
  (start [self]
    (log/info "-> starting AuthMiddleware")
    (let [auth-mw  (partial (or auth-mw no-auth-middleware) (:config config))
          new-self (assoc self :auth-mw auth-mw)]
      new-self))
  (stop [self]
    (log/info "<- stopping AuthMiddleware")
    self))

(defn new-auth-middleware
  ([auth-mw]
   (map->AuthMiddleware {:auth-mw auth-mw})))
