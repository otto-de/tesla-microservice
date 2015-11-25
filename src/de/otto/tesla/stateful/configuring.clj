(ns de.otto.tesla.stateful.configuring
  "This component is responsible for loading the configuration."
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.propertied.properties :as p]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [de.otto.tesla.util.keyword :as kwutil]
            [environ.core :as env :only [env]]
            [de.otto.tesla.util.env_var_reader :only [read-env-var]])
  (:import (java.io PushbackReader)))

(defn- load-properties-from-resource [resource]
  (kwutil/sanitize-keywords
    (p/properties->map
      (p/load-from resource) false)))

(defn- load-properties [name & [type]]
  (cond
    (and (= :properties type) (io/resource name)) (load-properties-from-resource (io/resource name))
    (and (= :file type) (.exists (io/file name))) (load-properties-from-resource (io/file name))))

(defn load-config-from-property-files []
  (let [defaults (load-properties "default.properties" :properties)
        config (load-properties (or (:config-file env/env) "application.properties") :file)
        local (load-properties "local.properties" :properties)]
    (merge defaults config local env/env)))

(defn- load-edn [name]
  (when-let [resource (io/resource name)]
    (-> resource
        (io/reader)
        (PushbackReader.)
        (read))))

(defn load-config-from-edn-files []
  (let [defaults (load-edn "default.edn")
        config (load-edn (or (:config-file env/env) "application.edn"))
        local (load-edn "local.edn")]
    (merge defaults config local)))

(defn load-and-merge [runtime-config]
  (if-not (:property-file-preferred runtime-config)
    (merge (load-config-from-edn-files) runtime-config)
    (merge (load-config-from-property-files) runtime-config)))

;; Load config on startup.
(defrecord Configuring [runtime-config]
  component/Lifecycle
  (start [self]
    (log/info "-> loading configuration.")
    (log/info runtime-config)
    (assoc self :config (load-and-merge runtime-config)
                :version (load-properties "version.properties" :properties)))

  (stop [self]
    (log/info "<- stopping configuration.")
    self))

(defn new-config [runtime-config]
  (map->Configuring {:runtime-config runtime-config}))

;; The hostname and port visble from the outside are different for
;; different environments.
;; These methods default to Marathon defaults.
(defn external-hostname [self]
  (let [conf (:config self)]
    (or (:host conf) (:host-name conf) (:hostname conf) "localhost")))

;; see above
(defn external-port [self]
  (let [conf (:config self)]
    (or (:port0 conf) (:host-port conf) (:server-port conf))))
