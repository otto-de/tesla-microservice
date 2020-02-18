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

(defn register-endpoint! [{metrics-path :metrics-path :as config} handler authenticate-type authenticate-fn]
  (log/info "Register metrics prometheus endpoint")
  (handlers/register-response-fn handler metrics-response (partial path-filter metrics-path) :authenticate-type authenticate-type :authenticate-fn authenticate-fn))
