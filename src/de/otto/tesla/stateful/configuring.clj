(ns de.otto.tesla.stateful.configuring
  "This component is responsible for loading the configuration."
  (:require [com.stuartsierra.component :as component]
            [clojure.tools.logging :as log]
            [gorillalabs.config :as config]
            [environ.core :as environ]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Access functions

(defn config
  ([config-component]
   (:config config-component)) ;; DO NOT USE, THIS IS FOR LEGACY SUPPORT ONLY!
  ([config-component key-path]
   (get-in (:config config-component) key-path))
  ([config-component key-path default]
   (get-in (:config config-component) key-path default)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Special access functions

(defn external-hostname [config-component]
  ;; old function was otto-specific
  (config config-component [:hostname] "localhost")
  )

(defn external-port [config-component]
  ;; old function was otto-specific
  (config config-component [:external-port]))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Component related stuff

(defn- load-config [_]
  (config/init (str (environ/env :system) "-" (environ/env :env))))

;; Load config on startup.
(defrecord Configuring [runtime-config load-config-fn]
  component/Lifecycle
  (start [self]
    (log/info "-> loading configuration.")
    (assoc self :config (merge (load-config-fn self) runtime-config)
                :version {:version "test.version"
                          :commit  "test.githash"}))

  (stop [self]
    (log/info "<- stopping configuration.")
    self))

(defn new-config [runtime-config & {:keys [load-config-fn] :or {load-config-fn load-config}}]
  (map->Configuring {:runtime-config runtime-config
                     :load-config-fn load-config-fn
                     }))

