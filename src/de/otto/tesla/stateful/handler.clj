(ns de.otto.tesla.stateful.handler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [metrics.core :as mcore])
  (:import (java.util.concurrent TimeUnit)
           (java.net URI)
           (com.codahale.metrics MetricRegistry SlidingTimeWindowReservoir Timer)))

(defn- sliding-window-timer [reporting-time-window-in-min]
  (Timer. (SlidingTimeWindowReservoir. reporting-time-window-in-min TimeUnit/MINUTES)))

(defn- register-timer [timer mname]
  (.register ^MetricRegistry mcore/default-registry mname timer))

(defn- new-stored-and-registered-timer [timers reporting-time-window-in-min mname]
  (let [t (sliding-window-timer reporting-time-window-in-min)]
    (register-timer t mname)
    (swap! timers assoc mname t)
    t))

(defn- timer-for-id [{:keys [timers reporting-time-window-in-min]} t-id]
  (let [mname (mcore/metric-name t-id)]
    (or (get @timers mname)
        (new-stored-and-registered-timer timers reporting-time-window-in-min mname))))

(defn- new-handler-name [self]
  (str "tesla-handler-" (count @(:registered-handlers self))))

(defn- handler-execution-result [request {handler-fn :handler :as handler-map}]
  (when-let [response (handler-fn request)]
    (assoc handler-map :response response)))

(defn- first-handler-result [handlers request]
  (some (partial handler-execution-result request) handlers))

(def without-leading-and-trailing-slash #"/?(.*[^/])/?")

(defn- trimmed-uri-path [uri]
  (let [path (.getPath (URI. uri))]
    (second (re-matches without-leading-and-trailing-slash path))))

(defn- extract-uri-resources [{:keys [uri-resource-chooser-fn]} {:keys [uri]}]
  (uri-resource-chooser-fn
    (if-let [splittable (trimmed-uri-path uri)]
      (string/split splittable #"/")
      [])))

(defn- request-based-timer-id [reporting-base-path item request response]
  (concat
    reporting-base-path
    (extract-uri-resources item request)
    [(str (:status response))]))

(defn- report-request-timings! [{:keys [reporting-base-path] :as self} item request response time-taken]
  (let [timer-id (request-based-timer-id reporting-base-path item request response)]
    (-> (timer-for-id self timer-id)
        (.update time-taken TimeUnit/MILLISECONDS))))

(defn- time-taken [start-time]
  (- (System/currentTimeMillis) start-time))

(defn- single-handler-fn [{:keys [registered-handlers] :as self}]
  (fn [request]
    (let [start-time (System/currentTimeMillis)]
      (when-let [{:keys [response timed?] :as item} (first-handler-result @registered-handlers request)]
        (when timed?
          (report-request-timings! self item request response (time-taken start-time)))
        response))))

(defprotocol HandlerContainer
  (register-handler [self handler])
  (register-timed-handler [self handler] [self handler uri-resource-chooser-fn])
  (handler [self]))


(def all-resources identity)
(def all-but-last-resource butlast)

(defrecord Handler [config]
  component/Lifecycle
  (start [self]
    (log/info "-> starting Handler")
    (assoc self
      :reporting-base-path (get-in config [:config :handler :reporting-base-path] ["serving" "requests"])
      :reporting-time-window-in-min (get-in config [:config :handler :reporting-time-window-in-min] 1)
      :timers (atom {})
      :registered-handlers (atom [])))

  (stop [self]
    (log/info "<- stopping Handler")
    self)

  HandlerContainer
  (register-handler [self handler]
    (swap! (:registered-handlers self) #(conj % {:timed?       false
                                                 :handler-name (new-handler-name self)
                                                 :handler      handler})))

  (register-timed-handler [self handler]
    (register-timed-handler self handler all-resources))

  (register-timed-handler [self handler uri-resource-chooser-fn]
    (swap! (:registered-handlers self) #(conj % {:timed?                  true
                                                 :handler-name            (new-handler-name self)
                                                 :uri-resource-chooser-fn uri-resource-chooser-fn
                                                 :handler                 handler})))

  (handler [self]
    (single-handler-fn self)))

(defn new-handler []
  (map->Handler {}))
