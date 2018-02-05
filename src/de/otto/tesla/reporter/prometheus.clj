(ns de.otto.tesla.reporter.prometheus
  (:require [de.otto.tesla.stateful.handler :as handler]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [de.otto.goo.goo :as goo]
            [de.otto.tesla.stateful.handler :as handlers]))

(defn- metrics-response [_]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (goo/text-format)})

(defn- path-filter [metrics-path handler]
  (c/GET metrics-path request (handler request)))

(defn- make-handler
  [prometheus-config authenticate-fn]
  (handlers/register-response-fn metrics-response
                                 (partial path-filter prometheus-config)
                                 :authenticate-fn authenticate-fn))

(defn register-endpoint! [{metrics-path :metrics-path} handler authenticate-fn]
  (log/info "Register metrics prometheus endpoint")
  (handler/register-handler handler (make-handler metrics-path authenticate-fn)))
