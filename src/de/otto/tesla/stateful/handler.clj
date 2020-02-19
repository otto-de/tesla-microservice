(ns de.otto.tesla.stateful.handler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [clojure.string :as string]
            [de.otto.goo.goo :as goo]
            [metrics.core :as mcore]
            [ring.middleware.basic-authentication :as ba]
            [ring.middleware.reload :refer [wrap-reload]])
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

(defn- new-handler-name [registered-handlers]
  (str "tesla-handler-" (count registered-handlers)))

(defn- handler-execution-result [request {handler-fn :handler :as handler-map}]
  (when-let [response (handler-fn request)]
    (assoc handler-map :response response)))

(defn- first-handler-result [handlers request]
  (some (partial handler-execution-result request) handlers))

(defn- report-request-timings! [timer-id self time-taken]
  (-> (timer-for-id self timer-id)
      (.update time-taken TimeUnit/MILLISECONDS)))

(defn- time-taken [start-time]
  (- (System/currentTimeMillis) start-time))

(defn- timer-path-fn-result [timer-path-fn request response]
  (try
    (some->> (timer-path-fn request response)
             (map str))
    (catch Exception e
      (log/error e "error executing the timer-path-fn in tesla-microservice: handler: " (.getMessage e)))))

(defn- single-handler-fn [{:keys [registered-handlers] :as self}]
  (fn [request]
    (let [start-time (System/currentTimeMillis)]
      (when-let [{:keys [response timer-path-fn]} (first-handler-result @registered-handlers request)]
        (some-> timer-path-fn
                (timer-path-fn-result request response)
                (report-request-timings! self (time-taken start-time)))
        response))))

(def without-leading-and-trailing-slash #"/?(.*[^/])/?")

(defn- trimmed-uri-path [uri]
  (let [path (.getPath (URI. uri))]
    (second (re-matches without-leading-and-trailing-slash path))))

(defn- extract-uri-resources [uri-resource-fn {:keys [uri]}]
  (uri-resource-fn
    (if-let [splittable (trimmed-uri-path uri)]
      (string/split splittable #"/")
      [])))

(defn- request-based-timer-id [reporting-base-path uri-resource-fn request response]
  (concat
    reporting-base-path
    (extract-uri-resources uri-resource-fn request)
    [(str (:status response))]))

(defn- lookup-uri-resource-fn [uri-resource-fn-or-keyword]
  (cond (keyword? uri-resource-fn-or-keyword)
        (uri-resource-fn-or-keyword {:all-but-last-resource butlast
                                     :all-resources         identity})
        :default uri-resource-fn-or-keyword))

(defn- default-timer-path-fn [reporting-base-path uri-resource-fn-or-keyword]
  (fn [request response]
    (request-based-timer-id
      reporting-base-path
      (lookup-uri-resource-fn uri-resource-fn-or-keyword)
      request response)))

(defn exceptions-to-500 [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Will return 500 to client because of this error.")
        {:status 500
         :body   (.getMessage e)}))))

(defn register-timed-handler [self new-handler-fn & {:keys [uri-resource-fn-or-keyword timer-path-fn]
                                                     :or   {uri-resource-fn-or-keyword :all-resources}}]
  (let [{:keys [registered-handlers reporting-base-path]} self
        handler-name (new-handler-name @registered-handlers)]
    (swap! registered-handlers
           #(conj % {:handler-name  handler-name
                     :timer-path-fn (or timer-path-fn
                                        (default-timer-path-fn reporting-base-path uri-resource-fn-or-keyword))
                     :handler       new-handler-fn}))))

(defn register-handler [{:keys [registered-handlers]} new-handler-fn]
  (let [handler-name           (new-handler-name @registered-handlers)
        extended-route-handler (exceptions-to-500 new-handler-fn)]
    (swap! registered-handlers #(conj % {:handler-name handler-name
                                         :handler      extended-route-handler}))))

(defn req-wants-basic-auth [request]
  (some? (some->> (:headers request)
                  (clojure.walk/keywordize-keys)
                  (:authorization)
                  (re-find #"^Basic (.*)$")
                  (last))))

(defn- wrap-auth [handler-fn authenticate-type authenticate-fn config]
  (fn [request]
    (cond (nil? authenticate-fn) handler-fn
          (not= :keycloak authenticate-type) (#(ba/wrap-basic-authentication handler-fn (partial authenticate-fn config)))
          (and (= :keycloak authenticate-type) (req-wants-basic-auth request)) (#(ba/wrap-basic-authentication handler-fn (partial authenticate-fn config)))
          :else handler-fn)))

(defn- wrap-instrumentation [handler-fn instrumentation?]
  (if instrumentation?
    (goo/timing-middleware handler-fn)
    handler-fn))

(defn register-response-fn [self response-fn path-filter
                            & {authenticate-type :authenticate-type
                               authenticate-fn   :authenticate-fn
                               instrumentation?  :instrumentation?
                               :or               {authenticate-fn   nil
                                                  instrumentation?  true
                                                  authenticate-type nil}}]
  (-> response-fn
      (wrap-instrumentation instrumentation?)
      (wrap-auth authenticate-type authenticate-fn (get-in self [:config :config]))
      (path-filter)
      (#(register-handler self %))))

(defn handler [{:keys [config] :as self}]
  (if (get-in config [:config :handler :hot-reload?])
    (wrap-reload (single-handler-fn self))
    (single-handler-fn self)))

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
    self))

(defn new-handler []
  (map->Handler {}))
