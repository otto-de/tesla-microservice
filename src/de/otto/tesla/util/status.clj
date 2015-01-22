(ns de.otto.tesla.util.status)

(defonce score {:ok      0
                :warning 1
                :error   2})

(defn better-of-two [s1 s2]
  (if (> (s1 score) (s2 score))
    s2
    s1))

(defn best-status [list]
  (reduce better-of-two list))

(defn worse-of-two [s1 s2]
  (if (< (s1 score) (s2 score))
    s2
    s1))

(defn worst-status [list]
  (reduce worse-of-two list))

(defn aggregate-forgiving [ok-msg err-msg a-map]
  (let [best (best-status (map :status (vals a-map)))
        result (if (= best :ok)
                 {:status :ok :message ok-msg}
                 {:status :error :message err-msg})]
    (assoc result :statusDetails a-map)))

(defn aggregate-strictly [ok-msg warn-msg err-msg a-map]
  (let [worst (worst-status (map :status (vals a-map)))
        result (if (= worst :ok)
                 {:status :ok :message ok-msg}
                 (if (= worst :warning)
                   {:status :warning :message warn-msg}
                   {:status :error :message err-msg}))]
    (assoc result :statusDetails a-map)))

(defn status-detail
  ([id status message]
    (status-detail id status message {}))
  ([id status message extras]
    {id (assoc extras :status status :message message)}))

(defn forgiving-strategy [list]
  (aggregate-forgiving "at least one ok" "none ok" list))

(defn strict-strategy [list]
  (aggregate-strictly "all ok" "some warnings" "none ok" list))

(defn aggregate-status
  ([id strategy funs] (aggregate-status id strategy funs {}))
  ([id strategy funs extras]
    {id
     (into extras
           (if (empty? funs)
             {:status :ok :message "no substatus"}
             (strategy (into {} (map #(%) funs)))))}))
