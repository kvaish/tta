(ns tta.util.common
  (:require [re-frame.core :as rf]
            [tta.app.subs :as app-subs]
            [clojure.string :as str]
            [ht.app.subs :as ht-subs]))

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
