(ns de.otto.tesla.stateful.health
  "This component provides a health-check endpoint which can be used to orchestrate app shutdown with load balancers."
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.handler :as handler]
            [de.otto.goo.goo :as goo]))

(def healthy-response {:status  200
                       :headers {"Content-Type" "text/plain"}
                       :body    "HEALTHY"})

(def unhealthy-response {:status  423
                         :headers {"Content-Type" "text/plain"}
                         :body    "UNHEALTHY"})

(defn health-response [self _]
  (if @(:locked self)
    unhealthy-response
    healthy-response))

(defn path-filter [self handler]
  (let [health-path (get-in self [:config :config :health-url] "/health")]
    (c/GET health-path request (handler request))))

(defn make-handler
  [self]
  (->> (partial health-response self)
       goo/timing-middleware
       (path-filter self)))

(defn lock-application [self]
  (goo/update! :health/locked 0)
  (reset! (:locked self) true))

(defrecord Health [config handler]
  component/Lifecycle
  (start [self]
    (log/info "-> Starting healthcheck.")
    (let [new-self (assoc self :locked (atom false))]
      (handler/register-handler handler (make-handler new-self)) ;; TODO: use config directly
      (goo/register-gauge! :health/locked {})
      (goo/update! :health/locked 1)
      new-self))

  (stop [self]
    (log/info "<- Stopping Healthcheck")
    self))

(defn new-health []
  (map->Health {}))

