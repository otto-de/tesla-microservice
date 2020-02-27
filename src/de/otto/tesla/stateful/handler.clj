(ns de.otto.tesla.stateful.handler
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [ring.middleware.basic-authentication :as ba]
            [ring.middleware.reload :refer [wrap-reload]]
            [de.otto.goo.goo :as goo]))

(defn- new-handler-name [registered-handlers]
  (str "tesla-handler-" (count registered-handlers)))

(defn- single-handler-fn [{:keys [registered-handlers]}]
  (fn [request]
    (some (fn [h]
            ((:handler h) request)) @registered-handlers)))

(defn exceptions-to-500 [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Will return 500 to client because of this error.")
        {:status 500
         :body   (.getMessage e)}))))

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
      :registered-handlers (atom [])))

  (stop [self]
    (log/info "<- stopping Handler")
    self))

(defn new-handler []
  (map->Handler {}))
