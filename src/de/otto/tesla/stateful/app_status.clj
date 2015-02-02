(ns de.otto.tesla.stateful.app-status
  (:require [com.stuartsierra.component :as component]
            [compojure.core :as c]
            [clojure.data.json :as json :only [write-str]]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clj-time.local :as local-time]
            [environ.core :as env]
            [de.otto.tesla.stateful.routes :as handlers]
            [de.otto.status :as s]))

;; http response for a healthy system
(def healthy-response {:status  200
                       :headers {"Content-Type" "text/plain"}
                       :body    "HEALTHY"})
;; http response for an unhealthy system
(def unhealthy-response {:status  503
                         :headers {"Content-Type" "text/plain"}
                         :body    "UNHEALTHY"})

(defn keyword-to-status [kw]
  (str/upper-case (name kw)))

(defn status-details-to-json [details]
  (into {} (map
             (fn [[k v]] (let [newstatus (keyword-to-status (:status v))]
                           {k (assoc v :status newstatus)}))
             details)))

(defn system-infos [config]
  (let [host (if-let [host-from-config (:host-name config)]
               host-from-config
               "localhost")
        port (if-let [host-port (:host-port config)]
               host-port
               (:server-port config))]
    {:systemTime (local-time/format-local-time (local-time/local-now) :date-time-no-ms)
     :hostname   host
     :port       port}))

(defn sanitize-str [s]
  (apply str (map (fn [_] "*") (vec s))))

(defn sanitize-mapentry [checklist [k v]]
  (let [new-val (if (some true? (map #(.contains (name k) %) checklist))
                  (sanitize-str v)
                  v)]
    {k new-val}))

(defn sanitize [config checklist]
  (into {}
        (map (partial sanitize-mapentry checklist) config)))

(defn create-complete-status [self]
  (let [config (:config (:config self))
        version-info (:version (:config self))
        extra-info {:version       (:version version-info)
                    :git           (:commit version-info)
                    :configuration (sanitize config ["passwd" "pwd"])}]
    (assoc
      (s/aggregate-status :application
                          s/strict-strategy
                          @(:status-functions self)
                          extra-info)
      :system (system-infos config))))

(defn health-response [self]
  (if (= (:status (:application (create-complete-status self))) :error)
    unhealthy-response
    healthy-response))

(defn status-response-body [self]
  (-> (create-complete-status self)
      (update-in [:application :statusDetails] status-details-to-json)
      (update-in [:application :status] keyword-to-status)))


;; This should apply to the specification at
;; http://spec.otto.de/media_types/application_vnd_otto_monitoring_status_json.html .
;; Right now it applies only partially.
(defn status-response [self]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    (json/write-str (status-response-body self))})

(defn register-status-fun [self fun]
  (swap! (:status-functions self) #(conj % fun)))

(defn handlers
  [self]
  [(c/GET "/status" [_]
          (status-response self))
   (c/GET "/health" [_]
          (health-response self))])


(defrecord ApplicationStatus []
  component/Lifecycle
  (start [self]
    (log/info "-> starting Application Status")
    (let [new-self (assoc self
                     :status-functions (atom []))]
      (handlers/register-routes (:routes new-self) (handlers new-self))
      new-self))

  (stop [self]
    (log/info "<- stopping Application Status")
    self))

(defn new-app-status []
  (map->ApplicationStatus {}))
