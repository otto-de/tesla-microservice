(ns de.otto.tesla.util.status-test
  (:require [clojure.test :refer :all]
            [de.otto.tesla.util.status :as s]))

(def s-ok {:ok-subcomponent {:status :ok :message "all ok"}})
(def s-ok2 {:ok-subcomponent2 {:status :ok :message "all ok"}})
(def s-warn {:warn-subcomponent {:status :warning :message "a warning"}})
(def s-error {:error-subcomponent {:status :error :message "an error"}})

(def forgiving-msgs {:ok  "at least one substatus ok"
                     :error  "no substatus ok"})

(deftest create-a-forgiving-aggregate-status
  (testing "ok if all ok"
    (is (= {:status        :ok :message "at least one substatus ok"
            :statusDetails {:ok-subcomponent  {:status :ok :message "all ok"}
                            :ok-subcomponent2 {:status :ok :message "all ok"}}}
           (s/aggregate-forgiving forgiving-msgs (merge s-ok s-ok2)))))
  (testing "ok if any ok"
    (is (= {:status        :ok :message "at least one substatus ok"
            :statusDetails {:ok-subcomponent    {:status :ok :message "all ok"}
                            :error-subcomponent {:status :error :message "an error"}}}
           (s/aggregate-forgiving forgiving-msgs (merge s-ok s-error)))))

  (testing "error if none ok"
    (is (= {:status        :error :message "no substatus ok"
            :statusDetails {:warn-subcomponent  {:status :warning :message "a warning"}
                            :error-subcomponent {:status :error :message "an error"}}}
           (s/aggregate-forgiving forgiving-msgs (merge s-warn s-error))))))

(deftest create-an-aggregate-status-with-a-strategy
  (testing "ok if all ok"
    (let [fun1 (fn [] {:ok-subcomponent {:status :ok :message "all ok"}})
          fun2 (fn [] {:ok-subcomponent2 {:status :ok :message "all ok"}})]
      (is (= {:id {:status        :ok
                   :message       "at least one ok"
                   :statusDetails {:ok-subcomponent  {:status :ok :message "all ok"}
                                   :ok-subcomponent2 {:status :ok :message "all ok"}}}}
             (s/aggregate-status :id s/forgiving-strategy [fun1 fun2])))))
  (testing "it keeps extra info"
    (let [fun (fn [] {:ok-subcomponent {:status :ok :message "all ok"}})]
      (is (= {:id {:status        :ok
                   :message       "at least one ok"
                   :extra-key     "extra-value"
                   :statusDetails {:ok-subcomponent  {:status :ok :message "all ok"}}}}
             (s/aggregate-status :id s/forgiving-strategy [fun] {:extra-key "extra-value"}))))))

(def strict-msgs {:ok  "all substatus ok"
                  :warning  "at least one substatus warn. no error"
                  :error  "at least one substatus error"})

(deftest create-a-strict-aggregate-status
  (testing "ok-if-all-ok"
    (is (= {:status        :ok :message "all substatus ok"
            :statusDetails {:ok-subcomponent  {:status :ok :message "all ok"}
                            :ok-subcomponent2 {:status :ok :message "all ok"}}}
           (s/aggregate-strictly
             strict-msgs (merge s-ok s-ok2)))))

  (testing "warn-if-any-warning"
    (is (= {:status        :warning :message "at least one substatus warn. no error"
            :statusDetails {:ok-subcomponent   {:status :ok :message "all ok"}
                            :warn-subcomponent {:status :warning :message "a warning"}}}
           (s/aggregate-strictly
             strict-msgs (merge s-ok s-warn)))))

  (testing "error-if-any-error"
    (is (= {:status        :error :message "at least one substatus error"
            :statusDetails {:ok-subcomponent    {:status :ok :message "all ok"}
                            :error-subcomponent {:status :error :message "an error"}
                            :warn-subcomponent  {:status :warning :message "a warning"}}}
           (s/aggregate-strictly
             strict-msgs (merge s-ok s-error s-warn))))))
