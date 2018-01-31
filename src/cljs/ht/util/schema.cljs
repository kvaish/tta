(ns ht.util.schema
  (:require [goog.date :as gdate]
            [ht.util.interop :as i]))


;;;;;;;;;;;;;;;;;;;;
;; custom parsers ;;
;;;;;;;;;;;;;;;;;;;;

(defn parse-date
  "parse date. returns nil if not valid."
  [txt]
  (if-let [txt (if (string? txt) (not-empty txt))]
    (let [d (js/Date. txt)]
      (if (pos? (i/ocall d :valueOf)) d))))

(defn parse-int
  "parse int. returns nil if not valid."
  [txt]
  (if-let [txt (if (string? txt) (not-empty txt))]
    (let [i (js/parseInt txt)]
      (if-not (js/isNaN i) i))))

(defn parse-bool
  "parse bool. returns nil if not valid."
  [txt]
  (if-let [txt (if (string? txt) (not-empty txt))]
    (case txt
      "true" true
      "false" false
      nil)))

;;;;;;;;;;;;;;;;;;;;;;;
;; custom formatters ;;
;;;;;;;;;;;;;;;;;;;;;;;


(defn format-date [date]
  (if date
    (-> date (gdate/DateTime.) (.toUTCRfc3339String))))


;;;;;;;;;;;;;;;;;;;;
;; schema fields  ;;
;;;;;;;;;;;;;;;;;;;;

(defn date-field [field-name]
  {:name field-name
   :overrides {:api {:parse parse-date
                     :unparse format-date}}})

(def id-field {:name "id"
               :overrides {:db {:name "_id"}}})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; entity converter to/from api ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


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


(defn- map-n-dim
  "n must be positive integer >= 1"
  ([f a n]
   (map-n-dim f a n nil))
  ([f a n res-f]
   (cond-> (mapv (if (> n 1)
                   #(map-n-dim f % (dec n) res-f)
                   f)
                 a)
     res-f res-f)))

(defn- from-js
  "Parse an entity from js object.  
  *schema-or-key* can be a map or a keyword. if keyword, it will be
  looked up from the *entity-schema*."
  [schema-or-key object entity-schema]
  (if-not object nil
          (if-let [schema (if (map? schema-or-key)
                            schema-or-key
                            (get entity-schema schema-or-key))]
            (first
             (reduce ;; parse the object for each key in schema
              (fn _parse_fn [[e o] [k a]]
                [(let [;; attribute name
                       n (cond
                           (string? a)  (keyword a)
                           (keyword? a) a
                           (map? a)     (let [n (:name a)]
                                          (if (keyword? n) n (keyword n)))
                           :not-possible
                           (throw (ex-info "Invalid attribute definition!"
                                           {:attr-def a})))
                       ;; attribute value
                       v (get o (keyword n)) ;; (g/get o n)
                       ;; parse value if applicable
                       v (if (some? v) ;; discard undefined/null
                           (if-not (map? a)
                             v
                             (let [n-dim (get a :array-dim 1)
                                   parse (or (some->> [(:parse a)
                                                       (if-let [s (:schema a)]
                                                         #(from-js s % entity-schema))]
                                                      (filter fn?)
                                                      (not-empty)
                                                      (apply comp))
                                             identity)]
                               (if (:array? a)
                                 (map-n-dim parse v n-dim vec)
                                 (parse v)))))]
                   (if (some? v) (assoc e k v) e)) ;; set the attribute
                 o])
              [{} object] ;; start with empty {}
              schema))
            (throw (ex-info "Invalid schema!" {:schema-or-key schema-or-key})))))


(defn- to-js
  "Unparse an entity to js object.  
  *schema-or-key* can be a map or a keyword. if keyword, it will be
  looked up from the *entity-schema*"
  [schema-or-key entity entity-schema]
  (if-not entity nil
          (if-let [schema (if (map? schema-or-key)
                            schema-or-key
                            (get entity-schema schema-or-key))]
            (first
             (reduce ;; unparse the entity for each key in schema
              (fn _unparse_fn [[o e] [k a]]
                [(let [;; attribute name
                       n (cond
                           (string? a)  (keyword a)
                           (keyword? a) a
                           (map? a)     (let [n (:name a)]
                                          (if (keyword? n) n (keyword n)))
                           :not-possible
                           (throw (ex-info "Invalid attribute definition!"
                                           {:attr-def a})))
                       ;; attribute value
                       v (get e k)
                       ;; unparse value if applicable
                       v (if (some? v) ;; discard undefined/null
                           (if-not (map? a)
                             v
                             (let [n-dim (get a :array-dim 1)
                                   unparse (or (some->> [(if-let [s (:schema a)]
                                                           #(to-js s % entity-schema))
                                                         (:unparse a)]
                                                        (filter fn?)
                                                        (not-empty)
                                                        (apply comp))
                                               identity)]
                               (if (:array? a)
                                 (map-n-dim unparse v n-dim vec)
                                 (unparse v)))))]
                   (if (some? v) (assoc o n v) o))
                 e])
              [{} entity] ;; start with empty {}
              schema))
            (throw (ex-info "Invalid schema!" {:schema-or-key schema-or-key})))))
