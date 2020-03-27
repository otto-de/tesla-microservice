(ns de.otto.tesla.stateful.app-status
  "This component renders a status page consisting of instance- and configuration-info
   as well as dynamic status of other components"
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [clojure.data.json :as json :only [write-str]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clj-time.local :as local-time]
            [environ.core :as env]
            [de.otto.tesla.stateful.handler :as handlers]
            [de.otto.status :as s]
            [ring.middleware.basic-authentication :as ba]
            [de.otto.tesla.util.sanitize :as san]
            [de.otto.tesla.stateful.configuring :as configuring]
            [de.otto.goo.goo :as goo]
            [de.otto.tesla.stateful.auth :as auth]))

(defn keyword-to-status [kw]
  (str/upper-case (name kw)))

(defn status-details-to-json [details]
  (into {} (map
             (fn [[k v]]
               {k (update-in v [:status] keyword-to-status)})
             details)))

(defn system-infos [config]
  {:systemTime (local-time/format-local-time (local-time/local-now) :date-time-no-ms)
   :hostname   (configuring/external-hostname config)
   :port       (configuring/external-port config)})

(defn aggregation-strategy [config]
  (if (= (get-in config [:status-aggregation]) "forgiving")
    s/forgiving-strategy
    s/strict-strategy))

(defn create-complete-status [self]
  (let [config             (get-in self [:config :config])
        version-info       (get-in self [:config :version])
        aggregate-strategy (:status-aggregation self)
        extra-info         {:name          (:name config)
                            :version       (:version version-info)
                            :git           (:commit version-info)
                            :configuration (san/hide-passwds config)}]
    (assoc
      (s/aggregate-status :application
                          aggregate-strategy
                          @(:status-functions self)
                          extra-info)
      :system (system-infos (:config self)))))

(defn status-response-body [self]
  (-> (create-complete-status self)
      (update-in [:application :statusDetails] status-details-to-json)
      (update-in [:application :status] keyword-to-status)))

;; This should apply to the specification at
;; http://spec.otto.de/media_types/application_vnd_otto_monitoring_status_json.html .
;; Right now it applies only partially.
(defn status-response [self _]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    (json/write-str (status-response-body self))})

(defn register-status-fun [self fun]
  (swap! (:status-functions self) #(conj % fun)))

(defn path-filter [self handler]
  (let [status-path (get-in self [:config :config :status-url] "/status")]
    (c/GET status-path request (handler request))))

(defn mk-handler [{:keys [auth-mw] :as self}]
  (let [auth-mw (:auth-mw auth-mw)]
    ((->> (partial status-response self)
          (goo/timing-middleware)
          (auth-mw)
          (partial path-filter self)))))

(defrecord ApplicationStatus [config handler auth-mw]
  component/Lifecycle
  (start [self]
    (log/info "-> starting Application Status")
    (let [new-self (assoc self
                     :status-aggregation (aggregation-strategy (:config config))
                     :status-functions (atom []))]

      (handlers/register-handler handler (mk-handler new-self))
      (goo/register-gauge! :build/info {:labels [:version :revision] :description "Constant '1' value labeled by version and revision of the service."})
      (goo/inc! :build/info {:version (-> config :version :version) :revision (-> config :version :commit)})
      new-self))

  (stop [self]
    (log/info "<- stopping Application Status")
    self))

(defn new-app-status
  ([]
   (map->ApplicationStatus {})))
