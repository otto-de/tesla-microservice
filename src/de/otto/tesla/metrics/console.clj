(ns de.otto.tesla.metrics.console
  (:require [clojure.tools.logging :as log]
            [overtone.at-at :as at]
            [de.otto.tesla.stateful.scheduler :as sched]
            [de.otto.tesla.metrics.core :as metrics]))

(defn write-to-console [console-config]
  (log/info "Metrics Reporting:\n"  (metrics/text-format)))

(defn start! [console-config scheduler]
  (let [interval-in-ms (* 1000 (:interval-in-s console-config))]
    (log/info "Starting metrics console reporter")
    (at/every interval-in-ms #(write-to-console console-config ) (sched/pool scheduler) :desc "Console-Reporter")))
