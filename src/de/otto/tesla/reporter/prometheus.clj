(ns de.otto.tesla.reporter.prometheus
  (:require [de.otto.tesla.stateful.handler :as handler]
            [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [de.otto.goo.goo :as goo]))

(defn- metrics-response [_]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    (goo/text-format)})


(defn- path-filter [{metrics-path :metrics-path} handler]
  (c/GET metrics-path request (handler request)))

(defn- make-handler
  [prometheus-config]
  (->> metrics-response
       goo/timing-middleware
       (path-filter prometheus-config)))

(defn register-endpoint! [prometheus-config handler]
  (log/info "Register metrics prometheus endpoint")
  (handler/register-handler handler (make-handler prometheus-config)))
