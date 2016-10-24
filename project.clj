(defproject de.otto/tesla-microservice "0.5.1-SNAPSHOT"
  :description "basic microservice."
  :url "https://github.com/otto-de/tesla-microservice"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/license/LICENSE-2.0.html"}
  :scm {:name "git"
        :url  "https://github.com/otto-de/tesla-microservice"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.logging "0.3.1"]

                 [de.otto/status "0.1.0"]

                 [beckon "0.1.1"]
                 [clj-time "0.12.0"]
                 [clojurewerkz/propertied "1.2.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [compojure "1.5.1"]
                 [environ "1.1.0"]
                 [metrics-clojure "2.7.0"]
                 [metrics-clojure-graphite "2.7.0"]
                 [overtone/at-at "1.2.0"]
                 [ring/ring-core "1.5.0"]]

  :exclusions [org.clojure/clojure
               org.slf4j/slf4j-nop
               org.slf4j/slf4j-log4j12
               log4j
               commons-logging/commons-logging]
  :lein-release {:deploy-via :clojars}

  :test-selectors {:default     (constantly true)
                   :integration :integration
                   :unit        :unit
                   :all         (constantly true)}
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[javax.servlet/servlet-api "2.5"]
                                      [org.slf4j/slf4j-api "1.7.21"]
                                      [ch.qos.logback/logback-core "1.1.7"]
                                      [ch.qos.logback/logback-classic "1.1.7"]
                                      [ring-mock "0.1.5"]]
                       :plugins      [[lein-ancient "0.6.10"][lein-release/lein-release "1.0.9"]]}}
  :test-paths ["test" "test-resources"])
