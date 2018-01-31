(ns tta.schema.model
  (:require [ht.util.schema :as u]
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
;;  :array-dim - an positive integer (>=2) applicable for nested arrays.
;;               indicates the depth of nesting or the number of dimensions.
;;               ignored if :array? is not true
;;  :parse - optional function to transform from js
;;  :unparse - optional function to transform back to js
;;  :schema - if present, further parse as defined by it.
;;            can be a map or a keyword pointing to a defined entity
;;  :overrides - optional, a map of overrides of above arranged by case-key.
;;               use this to slightly alter for other cases like :db or :api
;;  :scope - optional, a set of keywords, speicifying use cases like :db or :api
;;
;; if both :schema and :parse present, parse is applied after schema
;; if both :schema and :unparse present, unparse is applied before schema
;; if :array? is true, :schema or :parse & :unparse applied to each one

(def ^:private entity-schema
  (merge
   sap-client/schema, sap-plant/schema
   user/schema, client/schema, plant/schema,
   dataset/schema, message/schema
   {:res/create ^:api {:new-id "newId"}
    :res/update ^:api {:modified? "isModified"}
    :res/delete ^:api {:found? "isFound"}

    :pyrometer {:serial-number       "serialNumber"
                :tube-emissivity     "tubeEmissivity"
                :date-of-calibration (u/date-field "dateOfCalibration")
                :wavelength          "wavelength"
                :name                "name"
                :emissivity-setting  "emissivitySetting"
                :id                  "id"}

    :bin {:id           u/id-field
          :coll-name    "collName"
          :data         "data"
          :date-created "dateCreated"
          :created-by   "createdBy"}

    :log {:id        u/id-field
          :client-id "clientId"
          :plant-id  "plantId"
          :type      "type"
          :params    "params"
          :date      (u/date-field "date")
          :by        "by"}

    :log/query ^:api {:utc-start (u/date-field "utcStart")
                      :utc-end   (u/date-field "utcEnd")}}))

(defn- apply-overrides [schema case-key]
  (if-not (map? schema)
    schema
    (reduce-kv
     (fn [m attr field]
       (if-not (map? field)
         (assoc m attr field)
         (if-not ((get field :scope identity) case-key)
           m ;; skip if field not in scope
           (as-> field $
             (if-let [overrides (get-in $ [:overrides case-key])]
               (merge $ overrides)
               $)
             (dissoc $ :overrides :scope)
             (if-let [schema (:schema $)]
               (assoc $ :schema (apply-overrides schema case-key))
               $)
             (assoc m attr $)))))
     {} schema)))

(defn- derive-schema [case-key]
  (->> entity-schema
       (filter #(let [m (meta (val %))]
                  (or (nil? m) (get m case-key))))
       (map (fn [[ekey schema]]
              [ekey (apply-overrides schema case-key)]))
       (into {})))


(def entity-schema-api (derive-schema :api))


;; API

(defn from-api [entity-key entity]
  (u/from-js entity-key entity entity-schema-api))

(defn to-api [entity-key entity]
  (u/to-js entity-key entity entity-schema-api))
