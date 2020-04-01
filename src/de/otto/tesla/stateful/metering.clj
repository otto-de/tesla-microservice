(ns de.otto.tesla.stateful.metering
  "This component handles exporting of (prometheus) metrics"
  (:require
    [com.stuartsierra.component :as component]
    [clojure.tools.logging :as log]
    [de.otto.goo.goo :as goo]
    [de.otto.tesla.stateful.handler :as handlers]
    [compojure.core :as c]
    [overtone.at-at :as at]
    [de.otto.tesla.stateful.auth :as auth]))

(defn write-to-console []
  (log/info "Metrics Reporting:\n" (goo/text-format)))

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

(defn register-metrics-endpoint [{metrics-path :metrics-path} {:keys [handler auth]}]
  (log/info "Register metrics prometheus endpoint")
  (handlers/register-handler handler ((->> (metrics-response handler)
                                           (goo/timing-middleware)
                                           (auth/wrap-auth auth)
                                           (partial path-filter metrics-path)))))

(defn- start-reporter! [{:keys [scheduler] :as self} [reporter-type reporter-config]]
  (case reporter-type
    :console (start-console-reporter reporter-config scheduler)
    :prometheus (register-metrics-endpoint reporter-config self)))

(defn- start-reporters! [{:keys [config] :as self}]
  (let [available-reporters (get-in config [:config :metrics])]
    (run! (partial start-reporter! self) available-reporters)))

(defrecord Metering [config handler scheduler auth]
  component/Lifecycle
  (start [self]
    (log/info "-> starting metering.")
    (goo/register-counter! :metering/errors {:labels [:error :metric-name]})
    (assoc self :reporters (start-reporters! self)))

  (stop [self]
    (log/info "<- stopping metering")
    self))

(defn new-metering
  ([]
   (map->Metering {})))
