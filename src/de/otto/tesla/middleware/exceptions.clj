(ns de.otto.tesla.middleware.exceptions
  (:require [clojure.tools.logging :as log]))

(defn exceptions-to-500 [handler]
  (fn [request]
    (try
      (handler request)
      (catch Exception e
        (log/error e "Will return 500 to client because of this error.")
        {:status 500
         :body   (.getMessage e)}))))