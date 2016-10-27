(ns de.otto.tesla.stateful.keep-alive
  "This component is responsible for keeping the system alive by creating a non-deamonized noop thread."
  (:require [clojure.tools.logging :as log]
            [com.stuartsierra.component :as component])
  (:import (java.util.concurrent CountDownLatch)))

(defn exit-keep-alive []
  (log/info "<- stopping keepalive thread: " (.getName (Thread/currentThread))))

(defn enter-keep-alive []
  (log/info "-> starting keepalive thread: " (.getName (Thread/currentThread))))

(defn wait-for-count-down-latch [cdl]
  (enter-keep-alive)
  (.await cdl)
  (exit-keep-alive))

(defn start-keep-alive-thread [cd-latch]
  (doto (Thread. ^Runnable (partial wait-for-count-down-latch cd-latch))
    (.start)))

(defrecord KeepAlive [cd-latch]
  component/Lifecycle
  (start [self]
    (log/info "-> starting keepalive")
    (let [cd-latch (CountDownLatch. 1)]
      (assoc self
        :thread (start-keep-alive-thread cd-latch)
        :cd-latch cd-latch)))

  (stop [self]
    (log/info "<- stopping keepalive")
    (.countDown cd-latch)
    (dissoc self :thread)))

(defn new-keep-alive []
  (map->KeepAlive {}))
