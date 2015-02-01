(ns de.otto.tesla.util.stateless
  (:require [com.stuartsierra.component]
            [clojure.tools.logging]))

(defmacro stateless-component
  "Creates a component with no state of its own to manage, but that depends on other components to function"
  [name dependencies & body]
  `(do (defrecord ~name ~dependencies 
         com.stuartsierra.component/Lifecycle
         (~'start [self#]
           (let [{:keys ~dependencies} self#]
             (clojure.tools.logging/info ~(str "-> Starting " name))
             ~@body
             self#))
         (~'stop [self#]
           (let [{:keys ~dependencies} self#]
             (clojure.tools.logging/info ~(str "<- stopping " name))
             self#)))
       (defn ~(symbol (str "new-" name)) []
         (com.stuartsierra.component/using (~(symbol (str "map->" name)) {}) ~(mapv keyword dependencies)))))


