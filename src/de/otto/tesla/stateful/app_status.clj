(ns de.otto.tesla.stateful.app-status
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [clojure.data.json :as json :only [write-str]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clj-time.local :as local-time]
            [environ.core :as env]
            [de.otto.tesla.stateful.handler :as handlers]
            [de.otto.tesla.stateful.metering :as metering]
            [de.otto.status :as s]
            [metrics.timers :as timers]
            [de.otto.tesla.stateful.configuring :as config]
            [ring.middleware.defaults :as ring-defaults]))



(defn keyword-to-status [kw]
  (str/upper-case (name kw)))

(defn status-details-to-json [details]
  (into {} (map
             (fn [[k v]]
               {k (update-in v [:status] keyword-to-status)})
             details)))

(defn system-infos [config]
  {:systemTime (local-time/format-local-time (local-time/local-now) :date-time-no-ms)
   :hostname   (config/external-hostname config)
   :port       (config/external-port config)})

(defn sanitize-str [s]
  (apply str (repeat (count s) "*")))

(defn sanitize-mapentry [checklist [k v]]
  {k (if (some true? (map #(.contains (name k) %) checklist))
       (sanitize-str v)
       v)})

(defn sanitize [config checklist]
  (into {}
        (map (partial sanitize-mapentry checklist) config)))

(defn aggregation-strategy [config]
  (if (= (config/config config [:status-aggregation]) "forgiving")
    s/forgiving-strategy
    s/strict-strategy))

(defn create-complete-status [self]
  (let [version-info (get-in self [:config :version]) ;; TODO: Make this a separate component to read stuff from MANIFEST.MF
        aggregate-strategy (:status-aggregation self)
        extra-info {:name          (config/config (:config self) [:name])
                    :version       (:version version-info)
                    :git           (:commit version-info)
                    :configuration (sanitize (config/config (:config self)) ["pwd" "passwd"])}]
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
(defn status-response [self]
  (timers/time! (:status-timer self)
                {:status  200
                 :headers {"Content-Type" "application/json"}
                 :body    (json/write-str (status-response-body self))}))

(defn register-status-fun [self fun]
  (swap! (:status-functions self) #(conj % fun)))

(defn make-handler
  [self]
  (let [status-path (config/config (:config self) [:status :path] "/status")]
    (c/routes (-> (c/GET status-path
                         []
                    (status-response self))
                  (ring-defaults/wrap-defaults
                    (assoc ring-defaults/site-defaults :session false
                                                       :cookies false
                                                       :static false
                                                       :proxy true))))))



(defrecord ApplicationStatus [config handler metering]
  component/Lifecycle
  (start [self]
    (log/info "-> starting Application Status")
    (let [new-self (assoc self
                     :status-timer (metering/timer! metering "status")
                     :health-timer (metering/timer! metering "health")
                     :status-aggregation (aggregation-strategy config)
                     :status-functions (atom []))]

      (handlers/register-handler handler (make-handler new-self))
      new-self))

  (stop [self]
    (log/info "<- stopping Application Status")
    self))

(defn new-app-status []
  (map->ApplicationStatus {}))
