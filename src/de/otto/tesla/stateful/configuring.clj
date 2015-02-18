(ns de.otto.tesla.stateful.configuring
  "This component is responsible for loading the configuration."
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.propertied.properties :as p]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [de.otto.tesla.util.keyword :as kwutil]
            [environ.core :as env :only [env]]))

(defn- load-properties-from-resource [resource]
  (kwutil/sanitize-keywords
    (p/properties->map
      (p/load-from resource) false)))

(defn- load-properties [name & [type]]
  (cond
    (and (= :file type) (.exists (io/file name))) (load-properties-from-resource (io/file name))
    (io/resource name) (load-properties-from-resource (io/resource name))))

(defn load-config []
  (let [defaults (load-properties "default.properties")
        config (load-properties (or (:config-file env/env) "application.properties") :file)
        local (load-properties "local.properties")]
    (merge defaults config local env/env)))

;; Load config on startup.
(defrecord Configuring [runtime-config]
  component/Lifecycle
  (start [self]
    (log/info "-> loading configuration.")
    (log/info runtime-config)
    (assoc self :config (merge (load-config) runtime-config)
           :version (load-properties "version.properties")))

  (stop [self]
    (log/info "<- stopping configuration.")
    self))

(defn new-config [runtime-config] (map->Configuring {:runtime-config runtime-config}))
