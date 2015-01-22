(ns de.otto.tesla.example.example-page
  (:require
    [com.stuartsierra.component :as c]
    [de.otto.tesla.stateful.routes :as routes]
    [de.otto.tesla.util.status :as status]
    [de.otto.tesla.stateful.app-status :as app-status]
    [de.otto.tesla.example.calculating :as calculating]
    [compojure.core :as compojure]
    [hiccup.core :as hiccup]))

(defn usage-page [self]
  (let [x (calculating/calculations (:calculator self))]
    (hiccup/html [:body [:h1 "TO UPPER CASE"]
                  [:div (str "call /example/foo to get FOO")]
                  [:div (str x " calculations so far")]])))

(defn result-page [self input]
  (let [result (calculating/calculate! (:calculator self) input)]
    (hiccup/html [:body [:h1 "TO UPPER CASE"]
                  [:div (str input " to upper case is " result)]])))


(defrecord ExamplePage []
  c/Lifecycle
  (start [self]
    (routes/register-routes (:routes self)
                            [(compojure/GET "/example" [_] (usage-page self))
                             (compojure/GET "/example/:input" [input] (result-page self input))])
    (app-status/register-status-fun (:app-status self)
      (fn [] (status/status-detail :example-page :ok "page is always fine")))
    self)
  (stop [self]
    self))

(defn new-example-page [] (map->ExamplePage {}))