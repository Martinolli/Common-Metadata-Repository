(ns cmr.umm-spec.migration.collection-progress-migration
  "Contains helper functions for migrating between different versions of UMM collection progress"
  (:require
   [clojure.set :as set] 
   [cmr.common.util :as util]
   [cmr.umm-spec.util :as umm-spec-util]))

(def mapping-up
  "Defines mappings of CollectionProgress values from v1.9 to v1.10."
  {"COMPLETE" "COMPLETE"
   "IN WORK" "ACTIVE"
   "PLANNED" "PLANNED"
   "NOT APPLICABLE" "NOT APPLICABLE"
   "NOT PROVIDED" "NOT PROVIDED"})

(defn migrate-up
  "Migrate CollectionProgress from string to enum."
  [c]
  (assoc c :CollectionProgress
           (get mapping-up
                (util/safe-uppercase (:CollectionProgress c)) 
                umm-spec-util/NOT-PROVIDED))) 

(defn migrate-down
  "Migrate from enum to string."
  [c]
  (assoc c :CollectionProgress
           (get (set/map-invert mapping-up)
                (util/safe-uppercase (:CollectionProgress c)))))

