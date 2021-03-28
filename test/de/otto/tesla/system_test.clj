(ns de.otto.tesla.system-test
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as c]
            [de.otto.tesla.util.test-utils :as u :refer [eventually]]
            [de.otto.tesla.system :as system]
            [ring.mock.request :as mock]
            [de.otto.tesla.stateful.handler :as handler]
            [de.otto.tesla.stateful.configuring :as configuring]
            [environ.core :as env]
            [overtone.at-at :as at]))

(deftest ^:unit should-start-base-system-and-shut-it-down
  (testing "start then shutdown using own method"
    (let [system-exit-calls (atom [])
          started (system/start (system/base-system {}))]
      (with-redefs [system/exit #(swap! system-exit-calls conj %)]
        (system/stop started)
        (is (= [0] @system-exit-calls))))))

(defrecord BombOnStartup []
  c/Lifecycle
  (start [_self]
    (throw (Exception. "boom!")))
  (stop [self]
    self))

(defn new-bomb-on-startup []
  (map->BombOnStartup {}))

(deftest ^:unit should-shutdown-on-error-while-starting
  (let [exploding-system (assoc (system/base-system {}) :bomb (c/using (new-bomb-on-startup) [:health]))
        system-exit-calls (atom [])]
    (with-redefs [system/exit #(swap! system-exit-calls conj %)]
      (system/start exploding-system)
      (is (= [1] @system-exit-calls)))))

(defrecord BombEverytime []
  c/Lifecycle
  (start [_self]
    (throw (Exception. "boom!")))
  (stop [_self]
    (throw (Exception. "boom!"))))

(defn new-bomb-on-everytime []
  (map->BombEverytime {}))

(deftest should-exit-jvm-if-stopping-system-fails
  (let [exploding-system (assoc (system/base-system {}) :bomb (c/using (new-bomb-on-everytime) [:health]))
        system-exit-calls (atom [])]
    (with-redefs [system/exit #(swap! system-exit-calls conj %)]
      (system/start exploding-system)
      (is (= [1] @system-exit-calls)))))

(defrecord BombOnShutdown []
  c/Lifecycle
  (start [self]
    self)
  (stop [_self]
    (throw (Exception. "boom!"))))

(defn new-bomb-on-shutdown []
  (map->BombOnShutdown {}))

(deftest ^:unit should-shutdown-on-error-while-stopping
  (let [exploding-system-on-stop (assoc (system/base-system {}) :bomb (c/using (new-bomb-on-shutdown) [:health]))
        system-exit-calls (atom [])]
    (with-redefs [system/exit #(swap! system-exit-calls conj %)]
      (system/stop (system/start exploding-system-on-stop))
      (is (= [1] @system-exit-calls)))))

(deftest should-lock-application-on-shutdown
  (testing "the lock is set"
    (u/with-started
      [started (system/base-system {:wait-ms-on-stop 10})]
      (let [healthcomp (:health started)]
        (with-redefs [system/exit #(println "System exit would be called with code " %)]
          (system/stop started))
        (is (= @(:locked healthcomp) true)))))

  (testing "it waits on stop"
    (u/with-started
      [started (system/base-system {:wait-seconds-on-stop 1})]
      (let [has-waited (atom false)]
        (with-redefs [system/wait! (fn [_] (reset! has-waited true))
                      system/exit #(println "System exit would be called with code " %)]
          (let [healthcomp (:health started)
                _ (system/stop started)]
            (is (= @(:locked healthcomp) true))
            (is (= @has-waited true))))))))


(deftest ^:integration should-substitute-env-variables-while-reading
  (with-redefs [env/env {:my-custom-status-url "/custom/status/path" :prop-without-fallback "some prop value"}]
    (u/with-started [started (system/base-system {})]
                    (testing "should load the status-path property from edn"
                      (is (= "/custom/status/path"
                             (:status-url (configuring/load-config-from-edn-files {})))))

                    (testing "should point to edn-configured custom status url"
                      (let [handlers (handler/handler (:handler started))
                            response (handlers (mock/request :get "/custom/status/path"))]
                        (is (= 200 (:status response)))))))

  (u/with-started [started (system/base-system {})]
                  (testing "should fallback to default for status path"
                    (is (= "/status"
                           (:status-url (configuring/load-config-from-edn-files {})))))))

(deftest the-scheduler-in-the-base-system
  (testing "should schedule and execute task NOW"
    (u/with-started [started (system/base-system {})]
                    (let [work-done (atom :no-work-done)
                          {:keys [scheduler]} started]
                      (at/after 0 #(reset! work-done :work-done!) (:pool scheduler))
                      (eventually (= :work-done! @work-done))))))
