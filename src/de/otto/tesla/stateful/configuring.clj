(ns de.otto.tesla.stateful.configuring
  "This component is responsible for loading the configuration."
  (:require [com.stuartsierra.component :as component]
            [clojurewerkz.propertied.properties :as p]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [de.otto.tesla.util.keyword :as kwutil]
            [environ.core :as env :only [env]]
            [de.otto.tesla.util.env_var_reader :only [read-env-var]]
            [de.otto.tesla.util.sanitize :as san])
  (:import (java.io PushbackReader)))

(defn deep-merge
  "Recursively merges maps. If vals are not maps, the last value wins."
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(defn- load-properties-from-resource [resource]
  (kwutil/sanitize-keywords
    (p/properties->map
      (p/load-from resource) false)))

(defn- load-properties [name & [type]]
  (cond
    (and (= :resource type) (io/resource name)) (load-properties-from-resource (io/resource name))
    (and (= :file type) (.exists (io/file name))) (load-properties-from-resource (io/file name))))

(defn load-config-from-property-files []
  (let [defaults (load-properties "default.properties" :resource)
        config (load-properties (or (:config-file env/env) "application.properties") :file)
        local (load-properties "local.properties" :resource)]
    (merge defaults config local)))

(defn- load-edn [name & [type]]
  (when-let [resource (cond
                        (= :resource type) (io/resource name)
                        (and (= :file type) (.exists (io/file name))) (io/file name))]
    (-> resource
        (io/reader)
        (PushbackReader.)
        (read))))

(defn load-config-from-edn-files []
  (let [defaults (load-edn "default.edn" :resource)
        application (load-edn (or (:config-file env/env) "application.edn") :file)
        local (load-edn "local.edn" :resource)
        configs (filter some? [defaults application local])]
    (apply deep-merge configs)))

(defn load-and-merge [runtime-config]
  (if-not (:property-file-preferred runtime-config)
    (deep-merge (load-config-from-edn-files) runtime-config)
    (merge (load-config-from-property-files) runtime-config (if (:merge-env-to-properties-config runtime-config) env/env {}))))

;; Load config on startup.
(defrecord Configuring [runtime-config]
  component/Lifecycle
  (start [self]
    (log/info "-> loading configuration.")
    (log/info runtime-config)
    (let [config (load-and-merge runtime-config)]
      (log/info "-> using configuration:\n" (with-out-str (clojure.pprint/pprint (san/hide-passwds config))))
      (assoc self :config config
                  :version (load-properties "version.properties" :resource))))

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
