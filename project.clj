(defproject de.otto.tesla/tesla-microservice "0.1"
            :description "basic microservice."
            :url "http://dev.otto.de"

            :dependencies [[org.clojure/clojure "1.6.0"]
                           [com.stuartsierra/component "0.2.2"]
                           [org.clojure/core.cache "0.6.4"]
                           [clojurewerkz/propertied "1.2.0"]
                           [org.clojure/data.json "0.2.5"]
                           [beckon "0.1.1"]
                           [overtone/at-at "1.2.0"]
                           [environ "1.0.0"]
                           [clj-time "0.8.0"]

                           ;; io
                           [ring "1.3.1"]
                           [compojure "1.2.0"]
                           [hiccup "1.0.5"]
                           [metrics-clojure "2.3.0"]
                           [metrics-clojure-graphite "2.3.0"]

                           ;; logging
                           [org.clojure/tools.logging "0.3.0"]
                           [org.slf4j/slf4j-api "1.7.7"]
                           [ch.qos.logback/logback-core "1.1.2"]
                           [ch.qos.logback/logback-classic "1.1.2"]
                           [net.logstash.logback/logstash-logback-encoder "3.4"]

                           ;; testing
                           [ring-mock "0.1.5"]
                           [http-kit.fake "0.2.1"]]

            :exclusions [org.clojure/clojure
                         org.slf4j/slf4j-nop
                         org.slf4j/slf4j-log4j12
                         log4j
                         commons-logging/commons-logging]

            :plugins [[lein-marginalia "0.8.0"]
                      [lein-environ "1.0.0"]]
            :main ^:skip-aot de.otto.tesla.example.example-system
            :aot [de.otto.tesla.util.escapingmessageconverter]
            :clean-targets [:target-path :compile-path "target"]
            :source-paths ["src" "example/src"]
            :java-source-paths ["src/java"]
            :test-selectors {:default     (constantly true)
                             :integration :integration
                             :unit        :unit
                             :all         (constantly true)}
            :profiles {:test {:aot [de.otto.tesla.util.escapingmessageconverter]
                              :env {:metering-reporter "console"
                                    :import-products   "false"
                                    :server-port       "9991"
                                    :cache-dir         "/tmp"}}
                       :meta {:env {:app-name :tesla-meta}}}
            :test-paths ["test" "test-resources" "example/test"])
