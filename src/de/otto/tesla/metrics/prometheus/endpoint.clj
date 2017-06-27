(ns de.otto.tesla.metrics.prometheus.endpoint
  (:require [de.otto.tesla.stateful.handler :as handler]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [de.otto.tesla.metrics.prometheus.core :as metrics]))

(defn metrics-response []
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (metrics/text-format)})

(defn make-handler [{metrics-path :metrics-path}]
  (c/routes (c/GET metrics-path [] (metrics-response))))


(defn register-endpoint! [prometheus-config handler]
  (log/info "Register metrics prometheus endpoint")
  (handler/register-handler handler (make-handler prometheus-config)))
