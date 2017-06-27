(ns de.otto.tesla.stateful.health
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.handler :as handler]
            [metrics.gauges :as gauge]
            [de.otto.tesla.metrics.prometheus.core :as metrics]
            [iapetos.core :as prom]))

;; http response for a healthy system
(def healthy-response {:status  200
                       :headers {"Content-Type" "text/plain"}
                       :body    "HEALTHY"})
;; http response for an unhealthy system
(def unhealthy-response {:status  423
                         :headers {"Content-Type" "text/plain"}
                         :body    "UNHEALTHY"})

(defn health-response [self]
  (if @(:locked self)
    unhealthy-response
    healthy-response))

(defn make-handler
  [self]
  (let [health-path (get-in self [:config :config :health-url] "/health")]
    (c/routes (c/GET health-path [] (health-response self)))))

(defn lock-application [self]
  (metrics/with-default-registry (prom/set :health/locked 0))
  (reset! (:locked self) true))

(defrecord Health [config handler]
  component/Lifecycle
  (start [self]
    (log/info "-> Starting healthcheck.")
    (let [new-self (assoc self :locked (atom false))]
      (handler/register-handler handler (make-handler new-self)) ;; TODO: use config directly
      (metrics/register+execute :health/locked (prom/gauge {}) (prom/set {} 1))
      new-self))

  (stop [self]
    (log/info "<- Stopping Healthcheck")
    self))

(defn new-health []
  (map->Health {}))

