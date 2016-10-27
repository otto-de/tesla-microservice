(ns de.otto.tesla.util.test-utils
  (:require
    [com.stuartsierra.component :as comp]
    [ring.mock.request :as mock]))

(defmacro eventually
  "Generic assertion macro, that waits for a predicate test to become true.
  'form' is a predicate test, that clojure.test/is can understand
  'timeout': optional; in ms; how long to wait in total (defaults to 5000)
  'interval' optional; in ms; how long to pause between tries (defaults to 10)

  Example:
  Since this will fail half of the time ...
    (is (= 1 (rand-int 2)))

  ... use this:
    (eventually (= 1 (rand-int 2)))"
  [form & {:keys [timeout interval]
           :or   {timeout  5000
                  interval 10}}]
  `(let [start-time# (System/currentTimeMillis)]
     (loop []
       (let [last-stats# (atom nil)
             pass?# (with-redefs [clojure.test/do-report (fn [s#] (reset! last-stats# s#))]
                      (clojure.test/is ~form))
             took-too-long?# (> (- (System/currentTimeMillis) start-time#) ~timeout)]
         (if (or pass?# took-too-long?#)
           (clojure.test/do-report @last-stats#)
           (do
             (Thread/sleep ~interval)
             (recur)))))))

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

(defn mock-request
  "merges additional arguments into a mock-request"
  [method url args]
  (let [request (mock/request method url)]
    (merge-with merge request args)))
