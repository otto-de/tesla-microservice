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
    (let [config (load-and-merge runtime-config)]
      (log/info "-> using configuration:\n" (with-out-str (clojure.pprint/pprint config)))
      (assoc self :config config
                  :version (load-properties "version.properties" :properties))))

  (stop [self]
    (log/info "<- stopping configuration.")
    self))

(defn new-config [runtime-config]
  (map->Configuring {:runtime-config runtime-config}))

;; The hostname and port visble from the outside are different for
;; different environments.
;; These methods default to Marathon defaults.
(defn external-hostname [{:keys [config]}]
  (or (:host-name config)
      (:host env/env) (:host-name env/env) (:hostname env/env)
      "localhost"))

;; see above
(defn external-port [{:keys [config]}]
  (or (:server-port config)
      (:port0 env/env) (:host-port env/env) (:server-port env/env)))
