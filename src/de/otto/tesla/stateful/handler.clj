(ns de.otto.tesla.stateful.handler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [metrics.timers :as timers])
  (:import (java.util.concurrent TimeUnit)))

(defprotocol HandlerContainer
  (register-handler [self handler] [self handler-name handler])
  (handler [self]))

(defn- new-handler-name [self]
  (str "tesla-handler-" (count @(:the-handlers self))))

(defn- handler-execution-result [request {:keys [handler-name handler]}]
  (when-let [response (handler request)]
    {:response     response
     :handler-name handler-name}))

(defn- first-handler-result [handlers request]
  (some (partial handler-execution-result request) handlers))

(defn- default-handler [handlers]
  (fn [request]
    (-> (first-handler-result handlers request) :response)))

(defn- report-request-timings! [handler-name time-taken]
  (-> (timers/timer ["handler" "requests" handler-name])
      (.update time-taken TimeUnit/MILLISECONDS)))

(defn- time-taken [start-time]
  (- (System/currentTimeMillis) start-time))

(defn- timed-handler [handlers]
  (fn [request]
    (let [start-time (System/currentTimeMillis)]
      (when-let [{:keys [response handler-name]} (first-handler-result handlers request)]
        (report-request-timings! handler-name (time-taken start-time))
        response))))

(defrecord Handler [config]
  component/Lifecycle
  (start [self]
    (log/info "-> starting Handler")
    (assoc self
      :report-timings? (get-in config [:config :handler :report-timings?])
      :the-handlers (atom [])))

  (stop [self]
    (log/info "<- stopping Handler")
    self)

  HandlerContainer
  (register-handler [self handler]
    (register-handler self (new-handler-name self) handler))

  (register-handler [self handler-name handler]
    (swap! (:the-handlers self) #(conj % {:handler-name handler-name
                                          :handler      handler})))

  (handler [self]
    (let [handlers @(:the-handlers self)]
      (if (:report-timings? self)
        (timed-handler handlers)
        (default-handler handlers)))))

(defn new-handler []
  (map->Handler {}))
