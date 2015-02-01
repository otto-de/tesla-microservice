(ns de.otto.tesla.example.example-system
  (:require [de.otto.tesla.system :as system]
            [de.otto.tesla.example.calculating :as calculating]
            [de.otto.tesla.example.example-page :as example-page]
            [com.stuartsierra.component :as c]
            [de.otto.tesla.example.remote-services :as remote-services])
  (:gen-class))

(defn example-calculation-function [input]
  (.toUpperCase input))

(defn example-system [runtime-config]
  (-> (system/empty-system (merge {:name "example-service"} runtime-config))
      (assoc :calculator
             (c/using (calculating/new-calculator example-calculation-function) [:metering :app-status]))
      (assoc :example-page
             (c/using (example-page/new-example-page) [:routes :calculator :app-status]))
      (assoc :remote-calc (remote-services/new-remote-calc)) 
      (c/system-using {:server [:remote-calc :example-page]})))

(defn -main
  "starts up the production system."
  [& args]
  (system/start-system (example-system{})))

