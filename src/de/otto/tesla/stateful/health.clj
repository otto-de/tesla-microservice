(ns de.otto.tesla.stateful.health
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.routes :as handlers]))

;; http response for a healthy system
(def healthy-response {:status  200
                       :headers {"Content-Type" "text/plain"}
                       :body    "HEALTHY"})
;; http response for an unhealthy system
(def unhealthy-response {:status  503
                         :headers {"Content-Type" "text/plain"}
                         :body    "UNHEALTHY"})

(defn health-response [self]
  (if @(:locked self)
    unhealthy-response
    healthy-response))

(defn handlers
  [self]
  [(c/GET (get-in self [:config :config :health-url] "/health") [_]
     (health-response self))])

(defn lock-application [self]
  (reset! (:locked self) true))

(defrecord Health [config routes]
  component/Lifecycle
  (start [self]
    (log/info "-> Starting healthcheck.")
    (let [new-self (assoc self :locked (atom false))]
      (handlers/register-routes routes (handlers new-self))
      new-self))

  (stop [self]
    (log/info "<- Stopping Healthcheck")
    self))

(defn new-health []
  (map->Health {}))

