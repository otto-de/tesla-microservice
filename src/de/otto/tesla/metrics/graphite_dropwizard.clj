(ns de.otto.tesla.metrics.graphite-dropwizard
  (:require [de.otto.tesla.stateful.configuring :as configuring]
            [clojure.tools.logging :as log]
            [metrics.reporters.graphite :as graphite])
  (:import (java.util.concurrent TimeUnit)
           (com.codahale.metrics MetricFilter)))

(defn- short-hostname [hostname]
  (re-find #"[^.]*" hostname))

(defn- graphite-host-prefix [config]
  (let [external-hostname (configuring/external-hostname {})
        hostname (if (:shorten-hostname? config)
                   (short-hostname external-hostname)
                   external-hostname)]
    (str (:prefix config) "." hostname)))

(defn- config [config]
  (merge config
         {:port          (Integer. ^String (:port config))
          :prefix        (graphite-host-prefix config)
          :rate-unit     TimeUnit/SECONDS
          :duration-unit TimeUnit/MILLISECONDS
          :filter        MetricFilter/ALL}))

(defn start! [registry reporter-config]
  (let [config (config reporter-config)
        reporter (graphite/reporter registry config)]

    (log/info "-> starting dropwizard graphite reporter:" config)
    (graphite/start reporter (:interval-seconds config))
    reporter))