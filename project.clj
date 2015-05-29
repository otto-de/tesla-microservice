(defproject de.otto/tesla-microservice "0.1.12"
            :description "basic microservice."
            :url "https://github.com/otto-de/tesla-microservice"
            :license { :name "Apache License 2.0" 
                       :url "http://www.apache.org/license/LICENSE-2.0.html"}
            :scm { :name "git"
 	           :url "https://github.com/otto-de/tesla-microservice"}
            :dependencies [[org.clojure/clojure "1.6.0"]
                           [com.stuartsierra/component "0.2.3"]
                           [clojurewerkz/propertied "1.2.0"]
                           [org.clojure/data.json "0.2.6"]
                           [beckon "0.1.1"]
                           [overtone/at-at "1.2.0"]
                           [environ "1.0.0"]
                           [clj-time "0.9.0"]

                           [de.otto/status "0.1.0"]

                           ;; io
                           [ring "1.3.2"]
                           [compojure "1.3.4"]
                           [hiccup "1.0.5"]
                           [metrics-clojure "2.5.1"]
                           [metrics-clojure-graphite "2.5.1"]

                           ;; logging
                           [org.clojure/tools.logging "0.3.1"]
                           [org.slf4j/slf4j-api "1.7.12"]
                           [ch.qos.logback/logback-core "1.1.3"]
                           [ch.qos.logback/logback-classic "1.1.3"]

                           ;; testing
                           [ring-mock "0.1.5"]]

            :exclusions [org.clojure/clojure
                         org.slf4j/slf4j-nop
                         org.slf4j/slf4j-log4j12
                         log4j
                         commons-logging/commons-logging]

            :plugins [[lein-environ "1.0.0"]]
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
                       :meta {:env {:app-name :tesla-meta}}
                       :uberjar {:aot :all}}
            :test-paths ["test" "test-resources" "example/test"])
