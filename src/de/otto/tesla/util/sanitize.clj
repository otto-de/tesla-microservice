(ns de.otto.tesla.util.sanitize)

(def checklist ["password" "pw" "passwd" "private"])

(defn hide-passwd [k v]
  (if (some true? (map #(.contains (name k) %) checklist))
    "***"
    v))

(defn hide-passwds [map]
  (reduce
    (fn [new-map [k v]] (assoc new-map k (if (map? v) (hide-passwds v) (hide-passwd k v))))
    {} map))