(ns de.otto.tesla.stateful.health
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [clojure.tools.logging :as log]
            [de.otto.tesla.stateful.handler :as handler]
            [ring.middleware.defaults :as ring-defaults]
            [de.otto.tesla.stateful.configuring :as config]))

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

(defn make-handler
  [self]
  (let [health-path (config/config (:config self) [:health :path] "/health")]
    (c/routes (-> (c/GET health-path
                         []
                    (health-response self))
                  (ring-defaults/wrap-defaults
                    (assoc ring-defaults/site-defaults :session false
                                                       :cookies false
                                                       :static false
                                                       :proxy true))))))
(defn lock-application [self]
  (reset! (:locked self) true))

(defrecord Health [config handler]
  component/Lifecycle
  (start [self]
    (log/info "-> Starting healthcheck.")
    (let [new-self (assoc self :locked (atom false))]
      (handler/register-handler handler (make-handler new-self)) ;; TODO: use config directly
      new-self))

  (stop [self]
    (log/info "<- Stopping Healthcheck")
    self))

(defn new-health []
  (map->Health {}))

