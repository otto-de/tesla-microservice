(ns de.otto.tesla.stateful.routes
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [compojure.core :as c]))

(defn register-routes [self routes]
    (swap! (:the-routes self) #(concat % routes)))

(defn routes
  [self]
  (let [routes (:the-routes self)]
    (c/routes
      (apply c/routes @routes))))

(defrecord Routes []
  component/Lifecycle
  (start [self]
    (log/info "-> starting Routes")
    (assoc self :the-routes (atom [])))

  (stop [self]
    (log/info "<- stopping Routes")
    self))

(defn new-routes []
  (map->Routes {}))
