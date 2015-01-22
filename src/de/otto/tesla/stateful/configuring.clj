(ns de.otto.tesla.stateful.configuring
  "This component is responsible for loading the configuration."
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.propertied.properties :as p]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [de.otto.tesla.util.keyword :as kwutil]
            [environ.core :as env :only [env]]))

(defn- load-properties-from-resource! [resource]
  (kwutil/sanitize-keywords
    (p/properties->map
      (p/load-from resource) false)))

;; Loading files from classpath works differently ...
(defn- load-properties-from-classpath! [filename]
  (let [resource (io/resource filename)]
    (if (not (nil? resource))
      (load-properties-from-resource! resource)
      {})))

;; ... than loading files from filesystem.
(defn- load-properties-from-file! [filename]
  (let [file (io/file filename)]
    (if (.exists file)
      (load-properties-from-resource! file)
      {})))

(defn load-config []
  (let [defaults (load-properties-from-classpath! "default.properties")
        config (load-properties-from-file! "application.properties")
        local (load-properties-from-classpath! "local.properties")
        merged (merge defaults config local env/env)]
    merged))

;; Load config on startup.
(defrecord Configuring [runtime-config]
  component/Lifecycle
  (start [self]
    (log/info "-> loading configuration.")
    (log/info runtime-config)
    (assoc self :config (merge (load-config) runtime-config)
           :version (load-properties-from-classpath! "version.properties")))

  (stop [self]
    (log/info "<- stopping configuration.")
    self))

(defn new-config [runtime-config] (map->Configuring {:runtime-config runtime-config}))
