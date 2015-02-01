(ns de.otto.tesla.util.status)

(defonce scores {:ok      0
                :warning 1
                :error   2})

(defn aggregate-forgiving [msgs a-map]
  (let [best (apply min-key scores (map :status (vals a-map)))
        score (if (= :ok best) :ok :error)]
    (assoc {:status score :message (score msgs)}
           :statusDetails a-map)))

(defn aggregate-strictly [msgs a-map]
  (let [worst (apply max-key scores (map :status (vals a-map)))]
    (assoc {:status worst :message (worst msgs)}
           :statusDetails a-map)))

(defn status-detail
  ([id status message]
    (status-detail id status message {}))
  ([id status message extras]
    {id (assoc extras :status status :message message)}))

(defn forgiving-strategy [list]
  (aggregate-forgiving {:ok "at least one ok" :error "none ok"} list))

(defn strict-strategy [list]
  (aggregate-strictly {:ok "all ok" :warnings "some warnings" :error "none ok"} list))

(defn aggregate-status
  ([id strategy funs] (aggregate-status id strategy funs {}))
  ([id strategy funs extras]
    {id
     (into extras
           (if (empty? funs)
             {:status :ok :message "no substatus"}
             (strategy (into {} (map #(%) funs)))))}))
