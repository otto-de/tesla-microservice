(ns de.otto.tesla.stateful.handler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [metrics.timers :as timers]
            [clojure.string :as string])
  (:import (java.util.concurrent TimeUnit)
           (java.net URI)))

(defprotocol HandlerContainer
  (register-handler [self handler])
  (register-timed-handler [self handler] [self handler uri-resource-chooser-fn use-status-codes?])
  (handler [self]))

(defn- new-handler-name [self]
  (str "tesla-handler-" (count @(:registered-handlers self))))

(defn- handler-execution-result [request {handler-fn :handler :as handler-map}]
  (when-let [response (handler-fn request)]
    (assoc handler-map :response response)))

(defn- first-handler-result [handlers request]
  (some (partial handler-execution-result request) handlers))

(def without-leading-and-trailing-slash #"/?(.*[^/])/?")

(defn trimmed-uri-path [uri]
  (let [path (.getPath (URI. uri))]
    (second (re-matches without-leading-and-trailing-slash path))))

(defn- extract-uri-resources [{:keys [uri-resource-chooser-fn]} {:keys [uri]}]
  (uri-resource-chooser-fn
    (if-let [splittable (trimmed-uri-path uri)]
      (string/split splittable #"/")
      [])))

(defn request-based-timer-id [reporting-base-path {:keys [use-status-codes?] :as item} request response]
  (concat
    reporting-base-path
    (extract-uri-resources item request)
    (when use-status-codes? [(str (:status response))])))

(defn- report-request-timings! [reporting-base-path item request response time-taken]
  (-> (timers/timer (request-based-timer-id reporting-base-path item request response))
      (.update time-taken TimeUnit/MILLISECONDS)))

(defn- time-taken [start-time]
  (- (System/currentTimeMillis) start-time))

(defn- single-handler-fn [reporting-base-path handlers]
  (fn [request]
    (let [start-time (System/currentTimeMillis)]
      (when-let [{:keys [response timed?] :as item} (first-handler-result handlers request)]
        (when timed?
          (report-request-timings! reporting-base-path item request response (time-taken start-time)))
        response))))

(defrecord Handler [config]
  component/Lifecycle
  (start [self]
    (log/info "-> starting Handler")
    (assoc self
      :reporting-base-path (get-in config [:config :handler :reporting-base-path] ["serving" "requests"])
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
    (register-timed-handler self handler identity true))

  (register-timed-handler [self handler uri-resource-chooser-fn use-status-codes?]
    (swap! (:registered-handlers self) #(conj % {:timed?                  true
                                                 :handler-name            (new-handler-name self)
                                                 :uri-resource-chooser-fn uri-resource-chooser-fn
                                                 :use-status-codes?       use-status-codes?
                                                 :handler                 handler})))

  (handler [self]
    (let [handlers @(:registered-handlers self)]
      (single-handler-fn (:reporting-base-path self) handlers))))

(defn new-handler []
  (map->Handler {}))
