(ns de.otto.tesla.reporter.prometheus
  (:require [de.otto.tesla.stateful.handler :as handlers]
            [clojure.tools.logging :as log]
            [compojure.core :as c]
            [de.otto.goo.goo :as goo]
            [de.otto.tesla.middleware.auth :as auth]))

(defn metrics-response [_]
  (fn [_request] {:status  200
                 :headers {"Content-Type" "text/plain"}
                 :body    (goo/text-format)}))

(defn- path-filter [metrics-path handler]
  (c/GET metrics-path request (handler request)))

(defn register-endpoint! [{metrics-path :metrics-path :as config} handler authenticate-type authenticate-fn]
  (log/info "Register metrics prometheus endpoint")
  (handlers/register-handler handler ((->> metrics-response
                                           (goo/timing-middleware)
                                           (auth/wrap-auth authenticate-type authenticate-fn config)
                                           (partial path-filter metrics-path)))))

