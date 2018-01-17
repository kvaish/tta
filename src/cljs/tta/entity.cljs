(ns tta.entity
  (:require [goog.object :as g]
            #_[tta. :as u]
            [tta.schema.sap-client :as sap-client]
            [tta.schema.sap-plant :as sap-plant]
            [tta.schema.dataset :as dataset]
            [tta.schema.client :as client]
            [tta.schema.user :as user]
            [tta.schema.plant :as plant]
            [tta.schema.message :as message]))


;; entity schema is a map of field keyword to definition
;; field definition can be a string or keyword or a map.
;; if string/keyword, it is the field name, value is read or write as is.
;; if map, it can have following properties:
;;  :name - the field name
;;  :array? - if true, parse to array
;;  :parse - optional function to transform from js
;;  :unparse - optional function to transform back to js
;;  :schema - if present, further parse as defined by it.
;;            can be a map or a keyword pointing to a defined entity
;;
;; if both :schema and :parse present, parse is applied after schema
;; if both :schema and :unparse present, unparse is applied before schema
;; if :array? is true, :schema or :parse & :unparse applied to each one

(def entity-schema
  (let [log    {:id        "id"
                :client-id "clientId"
                :plant-id  "plantId"
                :type      "type"
                :params    "params"
                :date      "date"
                :by        "by"}
        db-log (assoc log :date "date")

        pyrometer    {:serial-number       "serialNumber"
                      :gold-cup?           "isGoldCup"
                      :date-of-calibration "dateOfCalibration"
                      :wavelength          "wavelength"
                      :name                "name"
                      :emissivity          "emissivity"
                      :id                  "id"}
        ]
    (merge
     sap-client/schema, sap-plant/schema
     user/schema, client/schema, plant/schema,
     dataset/schema, message/schema
     {:res/create {:new-id "newId"}
      :res/update {:modified? "isModified"}

      :pyrometer    pyrometer


      :bin    {:id "id"}
      :db/bin {:id           "id"
               :coll-name    "collName"
               :data         "data"
               :date-created "dateCreated"
               :created-by   "createdBy"}

      :log       log
      :db/log    db-log
      #_:log/query #_{:utc-start {:name  "utcStart"
                              :parse u/parse-date}
                  :utc-end   {:name  "utcEnd"
                              :parse u/parse-date}}})))

(defn from-js
  "Parse an entity from js object.
  *schema-or-key* can be a map or a keyword. if keyword, it will be
  looked up from the *entity-schema*."
  [schema-or-key object]
  (if-not object nil
          (if-let [schema (if (map? schema-or-key)
                            schema-or-key
                            (get entity-schema schema-or-key))]
            (first
             (reduce ;; parse the object for each key in schema
              (fn _parse_fn [[e o] [k a]]
                [(let [;; attribute name
                       n (cond
                           (string? a)  a
                           (keyword? a) (name a)
                           (map? a)     (let [n (:name a)] (if (keyword? n) (name n) n))
                           :not-possible
                           (throw (ex-info "Invalid attribute definition!"
                                           {:attr-def a})))
                       ;; attribute value
                       v (g/get o n)
                       ;; parse value if applicable
                       v (if (some? v) ;; discard undefined/null
                           (if-not (map? a)
                             v
                             (if-let [parse (some->> [(:parse a)
                                                      (if-let [s (:schema a)]
                                                        #(from-js s %))]
                                                     (filter fn?)
                                                     (not-empty)
                                                     (apply comp))]
                               (if (:array? a) (mapv parse v) (parse v))
                               (if (:array? a) (vec v) v))))]
                   (if (some? v) (assoc e k v) e)) ;; set the attribute
                 o])
              [{} object] ;; start with empty {}
              schema))
            (throw (ex-info "Invalid schema!" {:schema-or-key schema-or-key})))))


(defn to-js
  "Unparse an entity to js object.
  *schema-or-key* can be a map or a keyword. if keyword, it will be
  looked up from the *entity-schema*"
  [schema-or-key entity]
  (if-not entity nil
          (if-let [schema (if (map? schema-or-key)
                            schema-or-key
                            (get entity-schema schema-or-key))]
            (first
             (reduce ;; unparse the entity for each key in schema
              (fn _unparse_fn [[o e] [k a]]
                [(let [;; attribute name
                       n (cond
                           (string? a)  a
                           (keyword? a) (name a)
                           (map? a)     (let [n (:name a)]
                                          (if (keyword? n) (name n) n))
                           :not-possible
                           (throw (ex-info "Invalid attribute definition!"
                                           {:attr-def a})))
                       ;; attribute value
                       v (get e k)
                       ;; unparse value if applicable
                       v (if (some? v) ;; discard undefined/null
                           (if-not (map? a)
                             v
                             (if-let [unparse (some->> [(if-let [s (:schema a)]
                                                          #(to-js s %))
                                                        (:unparse a)]
                                                       (filter fn?)
                                                       (not-empty)
                                                       (apply comp))]
                               (if (:array? a) (to-array (map unparse v)) (unparse v))
                               (if (:array? a) (to-array v) v))))]
                   (if (some? v) (g/set o n v)) ;; set the attribute
                   o)
                 e])
              [#js{} entity] ;; start with empty #js{}
              schema))
            (throw (ex-info "Invalid schema!" {:schema-or-key schema-or-key})))))
