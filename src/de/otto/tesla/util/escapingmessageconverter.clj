(ns de.otto.tesla.util.escapingmessageconverter
  (:import ch.qos.logback.classic.pattern.ClassicConverter)
  (:gen-class
    :extends ch.qos.logback.classic.pattern.ClassicConverter))

(defn -convert [this event]
  (clojure.string/replace (.getFormattedMessage event) "\"" "'"))
