(ns de.otto.tesla.example.remote-services
  (:require [de.otto.tesla.util.stateless :as stateless]
            [compojure.core :as compojure]
            [de.otto.tesla.example.calculating :as calc]
            [de.otto.tesla.stateful.routes :as rts]))

(defn slurp-body [h]
  (fn [req]
    (h (update-in req [:body] slurp))))

(stateless/stateless-component remote-calc
                               [routes calculator]
                               (rts/register-routes routes
                                                    [(compojure/GET "/calculations" []
                                                                    (str  (calc/calculations calculator)))
                                                     (slurp-body
                                                      (compojure/POST "/calculate" r
                                                                      (calc/calculate! calculator (:body r))))]))
