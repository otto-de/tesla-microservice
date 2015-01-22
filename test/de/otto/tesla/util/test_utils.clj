(ns de.otto.tesla.util.test-utils
  (:require [clojure.test :refer :all]
            [com.stuartsierra.component :as comp]
            [ring.mock.request :as mock]))


(defmacro with-started
  "bindings => [name init ...]

  Evaluates body in a try expression with names bound to the values
  of the inits after (.start init) has been called on them. Finally
  a clause calls (.stop name) on each name in reverse order."
  [bindings & body]
  (if (and
        (vector? bindings) "a vector for its binding"
        (even? (count bindings)) "an even number of forms in binding vector")
    (cond
      (= (count bindings) 0) `(do ~@body)
      (symbol? (bindings 0)) `(let [~(bindings 0) (comp/start ~(bindings 1))]
                                (try
                                  (with-started ~(subvec bindings 2) ~@body)
                                  (finally
                                    (comp/stop ~(bindings 0)))))
      :else (throw (IllegalArgumentException.
                     "with-started-system only allows Symbols in bindings")))
    (throw (IllegalArgumentException.
             "not a vector or bindings-count is not even"))))


(defn merged-map-entry [request args k]
  (let [merged (merge (k request) (k args))]
    {k merged}))

(defn mock-request [method url args]
  (let [request (mock/request method url)
        all-keys (keys args)
        new-input (map (partial merged-map-entry request args) all-keys)]
    (merge request (into {} new-input))))

(deftest testing-the-mock-request
  (testing "should create mock-request"
    (is (= (mock-request :get "url" {})
           {:headers        {"host" "localhost"}
            :query-string   nil
            :remote-addr    "localhost"
            :request-method :get
            :scheme         :http
            :server-name    "localhost"
            :server-port    80
            :uri            "url"})))
  (testing "should create mock-request"
    (is (= (mock-request :get "url" {:headers {"content-type" "application/json"}})
           {:headers        {"host"         "localhost"
                             "content-type" "application/json"}
            :query-string   nil
            :remote-addr    "localhost"
            :request-method :get
            :scheme         :http
            :server-name    "localhost"
            :server-port    80
            :uri            "url"}))))