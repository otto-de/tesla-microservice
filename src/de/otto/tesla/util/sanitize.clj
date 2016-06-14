(ns de.otto.tesla.util.sanitize)

(def checklist ["password" "pwd" "passwd"])

(defn sanitize-mapentry [checklist [k v]]
  {k (if (some true? (map #(.contains (name k) %) checklist))
       "***"
       v)})

(defn sanitize [map-with-secrets]
  (into {}
        (map (partial sanitize-mapentry checklist) map-with-secrets)))