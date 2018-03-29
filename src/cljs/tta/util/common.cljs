(ns tta.util.common
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [cljsjs.react-motion] ;; required to ensure load
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [reagent.core :as r]))

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
   :value value})

(defn missing-field []
  {:value nil, :valid? false
   :error #(translate [:validation :hint :required]
                      "* Required field")})

;; validation mulit-method (fn validate [field params])
;; each one will check further only if :valid? is true, otherwise skip it
(defmulti validate #(if (:valid? %1) (:type %2)))

(defn validate-field [field & validators]
  (reduce #(validate %1 %2) field (remove nil? validators)))

(defmethod validate nil [field _] field)

(defmethod validate :number
  [{:keys [value] :as field} _]
  (if (not (re-matches #"\d+" value))
    (assoc field
           :error #(translate [:validation :hint :number]
                              "Please enter a number only")
           :valid? false)
    ;; pass through
    field))

(defmethod validate :decimal
  [{:keys [value] :as field} {:keys [precision]}]
  (let [precision (if (> precision 0) precision nil)]
    (if (not (re-matches (if precision
                           (re-pattern (str "(\\d+(\\.\\d{0," precision
                                            "})?)|(\\.\\d{1," precision "})"))
                           #"(\d+(\.\d*)?)|(\.\d+)")
                         value))
      (assoc field
             :error #(translate [:validation :hint :decimal]
                                "Please enter a decimal number only")
             :valid? false)
      ;; pass through
      field)))

(defmethod validate :max-number
  [{:keys [value] :as field} {:keys [max]}]
  (if (> value max)
    (assoc field
           :error #(translate [:validation :hint :max-number]
                              "Please enter a number ≤ {max}"
                              {:max max})
           :valid? false)
    ;; pass through
    field))

(defmethod validate :min-number
  [{:keys [value] :as field} {:keys [min]}]
  (if (< value min)
    (assoc field
           :error #(translate [:validation :hint :min-number]
                              "Please enter a number ≥ {min}"
                              {:min min})
           :valid? false)
    ;; pass through
    field))

;; parse mulit-method (fn parse [field params])
;; parse method only checks the :valid? key, which when true implies
;; that the :value is in correct shape for parsing
(defmulti parse #(if (:valid? %1) (:type %2)))

(defn parse-value [field & parsers]
  (reduce #(parse %1 %2) field (remove nil? parsers)))

(defmethod parse nil [field _] field)

(defmethod parse :number [field _]
  (update field :value js/Number))

(defmethod parse :decimal [field _]
  (update field :value js/Number))

(defmethod parse :temp [field {:keys [temp-unit]}]
  (update field :value #(from-temp-unit % temp-unit)))

;; field setters
(defn set-field [db path value data data-path form-path required?]
  (cond-> db
    data-path (assoc-in data-path (assoc-in data path value))
    (and required? form-path) (update-in form-path assoc-in path
                                         (if (some? value)
                                                   (make-field value)
                                                   (missing-field)))))

(defn set-field-text [db path value data data-path form-path required?]
  (cond-> db
          data-path (assoc-in data-path (assoc-in data path value))
          (and required? form-path) (update-in form-path assoc-in path
                                               (if (not-empty value)
                                                   (make-field value)
                                                   (missing-field)))))

(defn treat-blank [required?]
  (if required?
    ;; show message but do not clear data
    [false nil, true (missing-field)]
    ;; optional! clear both data and form
    [true nil true nil]))

(defn set-field-temperature [db path value data data-path form-path
                             required? temp-unit]
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
            (and f? form-path) (update-in form-path assoc-in path field)
            (and d? data-path) (assoc-in data-path (assoc-in data path value)))))

(defn set-field-number
  ([db path value data data-path form-path required?]
   (set-field-number db path value data data-path form-path required? nil))
  ([db path value data data-path form-path required? {:keys [max min]}]
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
             (and f? form-path) (update-in form-path assoc-in path field)
             (and d? data-path) (assoc-in data-path (assoc-in data path value))))))

(defn set-field-decimal [db path value data data-path form-path required?
                         {:keys [max min precision]}]
  (let [[d? value f? field]
        (if-let [value (not-empty value)]
          ;; has value, check and update
          (let [f (validate-field (make-field value) {:type :decimal
                                                      :precision precision})
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
            (and f? form-path) (update-in form-path assoc-in path field)
            (and d? data-path) (assoc-in data-path (assoc-in data path value)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; react-motion

(def motion (r/adapt-react-class js/ReactMotion.Motion))
(def stag-motion (r/adapt-react-class js/ReactMotion.StaggeredMotion))
(def trans-motion (r/adapt-react-class js/ReactMotion.TransitionMotion))
(defn spring
  "with precision to 0.03 (default 0.01)"
  ([s]
   (js/ReactMotion.spring s #js{:precision 0.03}))
  ([s {:keys [stiffness damping precision]
       :or {stiffness 170, damping 26, precision 0.03}}]
   (js/ReactMotion.spring s #js{:stiffness stiffness
                                :damping damping
                                :precision precision})))
(defn spring-gentle [s] (js/ReactMotion.spring s js/ReactMotion.presets.gentle))
(defn spring-wobbly [s] (js/ReactMotion.spring s js/ReactMotion.presets.wobbly))
(defn spring-stiff [s] (js/ReactMotion.spring s js/ReactMotion.presets.stiff))
