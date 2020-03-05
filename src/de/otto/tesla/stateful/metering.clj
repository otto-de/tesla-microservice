(ns de.otto.tesla.stateful.metering
  (:require
    [com.stuartsierra.component :as component]
    [clojure.tools.logging :as log]
    [de.otto.goo.goo :as goo]
    [de.otto.tesla.middleware.auth :as auth]
    [de.otto.tesla.stateful.handler :as handlers]
    [compojure.core :as c]
    [overtone.at-at :as at]
    [de.otto.tesla.stateful.scheduler :as sched]))

(defn write-to-console []
  (log/info "Metrics Reporting:\n"  (goo/text-format)))

(defn start-console-reporter [console-config scheduler]
  (let [interval-in-ms (* 1000 (:interval-in-s console-config))]
    (log/info "Starting metrics console reporter")
    (at/every interval-in-ms write-to-console (:pool scheduler) :desc "Console-Reporter")))

(defn metrics-response [_]
  (fn [_request] {:status  200
                  :headers {"Content-Type" "text/plain"}
                  :body    (goo/text-format)}))

(defn- path-filter [metrics-path handler]
  (c/GET metrics-path request (handler request)))

(defn register-metrics-endpoint [{metrics-path :metrics-path :as config} handler authenticate-type authenticate-fn]
  (log/info "Register metrics prometheus endpoint")
  (handlers/register-handler handler ((->> metrics-response
                                           (goo/timing-middleware)
                                           (auth/wrap-auth authenticate-type authenticate-fn config)
                                           (partial path-filter metrics-path)))))

(defn- start-reporter! [handler scheduler authenticate-type prometheus-auth-fn [reporter-type reporter-config]]
  (case reporter-type
    :console (start-console-reporter reporter-config scheduler)
    :prometheus (register-metrics-endpoint reporter-config handler authenticate-type prometheus-auth-fn)))

(defn- start-reporters! [config handler scheduler authenticate-type prometheus-auth-fn]
  (let [available-reporters (get-in config [:config :metrics])]
    (run! (partial start-reporter! handler scheduler authenticate-type prometheus-auth-fn) available-reporters)))

(defrecord Metering [authenticate-type prometheus-auth-fn config handler scheduler]
  component/Lifecycle
  (start [self]
    (log/info "-> starting metering.")
    (goo/register-counter! :metering/errors {:labels [:error :metric-name]})
    (assoc self :reporters (start-reporters! config handler scheduler authenticate-type prometheus-auth-fn)))

  (stop [self]
    (log/info "<- stopping metering")
     self))

(defn new-metering
  ([]
   (new-metering nil))
  ([prometheus-auth-fn]
   (map->Metering {:prometheus-auth-fn prometheus-auth-fn}))
  ([authenticate-type prometheus-auth-fn]
   (map->Metering {:authenticate-type  authenticate-type
                   :prometheus-auth-fn prometheus-auth-fn})))
