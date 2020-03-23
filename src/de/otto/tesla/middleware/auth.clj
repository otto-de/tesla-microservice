(ns de.otto.tesla.middleware.auth
  "This middleware handles authentication."
  (:require [ring.middleware.basic-authentication :as ba]))

(defn req-wants-basic-auth [request]
  (some? (some->> (:headers request)
                  (clojure.walk/keywordize-keys)
                  (:authorization)
                  (re-find #"^Basic (.*)$")
                  (last))))

(defn wrap-auth [authenticate-type authenticate-fn config handler-fn]
  (fn [request]
    (cond (nil? authenticate-fn) handler-fn
          (not= :keycloak authenticate-type) (#(ba/wrap-basic-authentication handler-fn (partial authenticate-fn config)))
          (and (= :keycloak authenticate-type) (req-wants-basic-auth request)) (#(ba/wrap-basic-authentication handler-fn (partial authenticate-fn config)))
          :else handler-fn)))