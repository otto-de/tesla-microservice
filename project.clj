(defproject de.otto/tesla-microservice "0.17.0-SNAPSHOT"
  :description "basic microservice."
  :url "https://github.com/otto-de/tesla-microservice"
  :license {:name "Apache License 2.0"
            :url  "http://www.apache.org/license/LICENSE-2.0.html"}
  :scm {:name "git"
        :url  "https://github.com/otto-de/tesla-microservice"}
  :repositories [["releases" {:url   "https://repo.clojars.org"
                              :creds :gpg}]]
  :dependencies [[org.clojure/data.json "1.0.0"]
                 [org.clojure/tools.logging "1.1.0"]
                 [de.otto/status "0.1.3"]
                 [de.otto/goo "1.2.12"]
                 [beckon "0.1.1"]
                 [clj-time "0.15.2"]
                 [clojurewerkz/propertied "1.3.0"]
                 [com.stuartsierra/component "1.0.0"]
                 [compojure "1.6.2"]
                 [environ "1.2.0"]
                 [overtone/at-at "1.2.0"]
                 [ring/ring-core "1.8.2"]
                 [ring/ring-devel "1.8.2"]
                 [ring-basic-authentication "1.1.0"]]

  :exclusions [org.clojure/clojure
               org.slf4j/slf4j-nop
               org.slf4j/slf4j-log4j12
               log4j
               commons-logging/commons-logging]
  :lein-release {:deploy-via :clojars}

  :filespecs [{:type :path :path "test-utils"}]

  :test-selectors {:default     (constantly true)
                   :integration :integration
                   :unit        :unit
                   :all         (constantly true)}
  :profiles {:uberjar {:aot :all}
             :dev     {:dependencies [[org.clojure/clojure "1.10.1"]
                                      [org.slf4j/slf4j-api "1.7.30"]
                                      [ch.qos.logback/logback-core "1.2.3"]
                                      [ch.qos.logback/logback-classic "1.2.3"]
                                      [ring-mock "0.1.5"]
                                      [org.clojure/data.codec "0.1.1"]]
                       :plugins      [[lein-release/lein-release "1.0.9"]]}}
  :test-paths ["test" "test-resources" "test-utils"])
