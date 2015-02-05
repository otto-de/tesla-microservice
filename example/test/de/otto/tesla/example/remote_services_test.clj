(ns de.otto.tesla.example.remote-services-test
  (:require [clojure.test :refer :all]
            [ring.mock.request :as mock]
            [de.otto.tesla.util.test-utils :as u]
            [de.otto.tesla.stateful.routes :as rts]
            [de.otto.tesla.example.example-system :as example-system]))

(deftest ^:unit remote-calc
  (u/with-started [started (example-system/example-system {})]
    (let [routes (rts/routes (:routes started))
          res1 (routes (mock/body
                        (mock/request :post "/calculate")
                        "lowercaseword"))
          res2 (routes (mock/request :get "/calculations"))]
      (is (= res1 {:status 200 :body "LOWERCASEWORD" :headers {"Content-Type" "text/html; charset=utf-8"}}))
      (is (= res2 {:status 200 :body "1" :headers {"Content-Type" "text/html; charset=utf-8"}})))))
