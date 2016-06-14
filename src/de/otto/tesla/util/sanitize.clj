(ns de.otto.tesla.util.sanitize)

(def checklist ["password" "pwd" "passwd"])

(defn sanitize-str [s]
  (apply str (repeat (count s) "*")))

(defn sanitize-mapentry [checklist [k v]]
  {k (if (some true? (map #(.contains (name k) %) checklist))
       (sanitize-str v)
       v)})

(defn sanitize [map-with-secrets]
  (into {}
        (map (partial sanitize-mapentry checklist) map-with-secrets)))