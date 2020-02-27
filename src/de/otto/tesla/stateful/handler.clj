(ns de.otto.tesla.stateful.handler
  (:require [com.stuartsierra.component :as component]
            [de.otto.tesla.middleware.exceptions :as ex]
            [clojure.tools.logging :as log]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn- single-handler-fn [{:keys [registered-handlers]}]
  (fn [request]
    (some (fn [h] (h request)) @registered-handlers)))

(defn register-handler [{:keys [registered-handlers]} new-handler-fn]
  (swap! registered-handlers conj (ex/exceptions-to-500 new-handler-fn)))

(defn handler [{:keys [config] :as self}]
  (if (get-in config [:config :handler :hot-reload?])
    (wrap-reload (single-handler-fn self))
    (single-handler-fn self)))

(defrecord Handler [config]
  component/Lifecycle
  (start [self]
    (log/info "-> starting Handler")
    (assoc self :registered-handlers (atom [])))
  (stop [self]
    (log/info "<- stopping Handler")
    self))

(defn new-handler []
  (map->Handler {}))