(ns cmr.metadata-db.int-test.concepts.tag-association-save-test
  "Contains integration tests for saving tag associations. Tests saves with various configurations
   including checking for proper error handling."
  (:require
   [clojure.test :refer :all]
   [cmr.metadata-db.int-test.concepts.concept-save-spec :as c-spec]
   [cmr.metadata-db.int-test.utility :as util]))

(use-fixtures :each (util/reset-database-fixture {:provider-id "REG_PROV" :small false}))

(defmethod c-spec/gen-concept :tag-association
  [_ _ uniq-num attributes]
  (let [concept-attributes (or (:concept-attributes attributes) {})
        concept (util/create-and-save-collection "REG_PROV" uniq-num 1 concept-attributes)
        tag-attributes (or (:tag-attributes attributes) {})
        tag (util/create-and-save-tag uniq-num 1 tag-attributes)
        attributes (dissoc attributes :concept-attributes :tag-attributes)]
    (util/tag-association-concept concept tag uniq-num attributes)))

;; tests
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(deftest save-tag-association-test
  (c-spec/general-save-concept-test :tag-association ["CMR"]))

(deftest save-tag-association-failure-test
  (testing "saving tag associations on non system-level provider"
    (let [coll (util/create-and-save-collection "REG_PROV" 1)
          tag (util/create-and-save-tag 1)
          tag-association (-> (util/tag-association-concept coll tag 2)
                              (assoc :provider-id "REG_PROV"))
          {:keys [status errors]} (util/save-concept tag-association)]

      (is (= 422 status))
      (is (= [(str "Tag association could not be associated with provider [REG_PROV]. "
                   "Tag associations are system level entities.")]
             errors)))))
