(ns tta.util.common
  (:require [re-frame.core :as rf]
            [tta.app.subs :as app-subs]
            [clojure.string :as str]
            [ht.app.subs :as ht-subs :refer [translate]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; temperature units and conversion
(def ^:const deg-C "°C")
(def ^:const deg-F "°F")

(defn to-temp-unit
  "convert temperature from base unit to given unit. Round for display."
  [v temp-unit]
  (if (number? v)
    (js/Math.round
     (case temp-unit
       "°C" v
       "°F" (+ (* 1.8 v) 32)))))

(defn from-temp-unit
  "convert temperature to base unit from given unit. No rounding for storage."
  [v temp-unit]
  (if (number? v)
    (case temp-unit
      "°C" v
      "°F" (/ (- v 32) 1.8))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; parse parameters for translate
(defmulti param->str :type)

(defn parse-params [params]
  (reduce-kv (fn [m k v]
               (assoc m k (if (map? v) (param->str v) v)))
             {} params))

(defmethod param->str "temperature" [param]
  (let [uom @(rf/subscribe [::app-subs/temp-unit])]
    (get param (keyword uom))))

(defmethod param->str "translation" [param]
  (let [ks (map keyword (str/split (:key param) #"\."))]
    (or @(rf/subscribe [::ht-subs/translation ks]) "_")))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; used to check if any field invalid in the form
(defn some-invalid [form]
  (if (map? form)
    (if (boolean? (:valid? form))
      (not (:valid? form))
      (some some-invalid (remove nil? (vals form))))
    (if (coll? form)
      (some some-invalid (remove nil? form)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; form utils

(defn make-field [value]
  {:valid? true
   :error nil
   :value value})

(defn missing-field []
  {:value nil, :valid? false
   :error (translate [:validation :hint :required]
                     "* Required field")})

;; validation mulit-method (fn validate [field params])
;; each one will check further only if :valid? is true, otherwise skip it
(defmulti validate #(:type %2))

(defn validate-field [field & validators]
  (reduce #(validate %1 %2) field (remove nil? validators)))

(defmethod validate :number
  [{:keys [value valid?] :as field} _]
  (if (and valid?
           (not (re-matches #"\d+" value)))
    (assoc field
           :error (translate [:validation :hint :number]
                             "Please enter a number only")
           :valid? false)
    ;; pass through
    field))

(defmethod validate :decimal
  [{:keys [value valid?] :as field} _]
  (if (and valid?
           (not (re-matches #"(\d+(\.\d*)?)|(\.\d+)" value)))
    (assoc field
           :error (translate [:validation :hint :decimal]
                             "Please enter a decimal number only")
           :valid? false)
    ;; pass through
    field))

(defmethod validate :max-number
  [{:keys [value valid?] :as field} {:keys [max]}]
  (if (and valid? (> value max))
    (assoc field
           :error (translate [:validation :hint :max-number]
                             "Please enter a number smaller than {max}"
                             {:max max})
           :valid? false)
    ;; pass through
    field))

(defmethod validate :min-number
  [{:keys [value valid?] :as field} {:keys [min]}]
  (if (and valid? (< value min))
    (assoc field
           :error (translate [:validation :hint :min-number]
                             "Please enter a number greater than {min}"
                             {:min min})
           :valid? false)
    ;; pass through
    field))

;; parse mulit-method (fn parse [field params])
;; parse method only checks the :valid? key, which when true implies
;; that the :value is in correct shape for parsing
(defmulti parse #(:type %2))

(defn parse-value [field & parsers]
  (reduce #(parse %1 %2) field (remove nil? parsers)))

(defmethod parse :number
  [{:keys [value valid?] :as field} _]
  (if valid?
    (assoc field :value (js/Number value))
    field))

(defmethod parse :decimal
  [{:keys [value valid?] :as field} _]
  (if valid?
    (assoc field :value (js/Number value))
    field))

(defmethod parse :temp
  [{:keys [value valid?] :as field} {:keys [temp-unit]}]
  (if valid?
    (assoc field :value (from-temp-unit value temp-unit))
    field))

(defn set-field [db path value data data-path form-path required?]
  (cond-> db
    :always (assoc-in data-path (assoc-in data path value))
    required? (update-in form-path assoc-in path (if (some? value)
                                                   (make-field value)
                                                   (missing-field)))))

(defn set-text-field [db path value data data-path form-path required?]
  (cond-> db
    :always (assoc-in data-path (assoc-in data path value))
    required? (update-in form-path assoc-in path (if (not-empty value)
                                                   (make-field value)
                                                   (missing-field)))))

(defn treat-blank [required?]
  (if required?
    ;; show message but do not clear data
    [false nil, true (missing-field)]
    ;; optional! clear both data and form
    [true nil true nil]))

(defn set-field-temperature [db path value data data-path form-path required? temp-unit]
  (let [[d? value f? field]
        (if-let [value (not-empty value)]
          ;; has value, check and update
          (let [f (validate-field (make-field value) {:type :number})
                v (parse-value f {:type :number}
                               {:type :temp, :temp-unit temp-unit})]
            ;; update data when valid and block typing when invalid number
            [(:valid? v) (:value v) (:valid? f) f])
          ;; blank!
          (treat-blank required?))]
    (cond-> db
      f? (update-in form-path assoc-in path field)
      d? (assoc-in data-path (assoc-in data path value)))))

(defn set-field-number [db path value data data-path form-path required?
                        {:keys [max min]}]
  (let [[d? value f? field]
        (if-let [value (not-empty value)]
          ;; has value, check and update
          (let [f (validate-field (make-field value) {:type :number})
                v (parse-value f {:type :number})
                f2 (validate-field v
                                   (if max {:type :max-number, :max max})
                                   (if min {:type :min-number, :min min}))]
            ;; update data when valid and block typing when invalid number
            [(:valid? f2) (:value v) (:valid? f) (assoc f2 :value value)])
          ;; blank!
          (treat-blank required?))]
    (cond-> db
      f? (update-in form-path assoc-in path field)
      d? (assoc-in data-path (assoc-in data path value)))))

(defn set-field-decimal [db path value data data-path form-path required?
                         {:keys [max min]}]
  (let [[d? value f? field]
        (if-let [value (not-empty value)]
          ;; has value, check and update
          (let [f (validate-field (make-field value) {:type :decimal})
                v (parse-value f {:type :decimal})
                f2 (validate-field v
                                   (if max {:type :max-number, :max max})
                                   (if min {:type :min-number, :min min}))]
            ;; update data when valid and block typing when invalid number
            [(:valid? f2) (:value v)
             (or (:valid? f) (= "." value)) (assoc f2 :value value)])
          ;; blank!
          (treat-blank required?))]
    (cond-> db
      f? (update-in form-path assoc-in path field)
      d? (assoc-in data-path (assoc-in data path value)))))
