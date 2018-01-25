(ns tta.util.common
  (:require [re-frame.core :as rf]
            [tta.app.subs :as app-subs]
            [clojure.string :as str]
            [ht.app.subs :as ht-subs]))

(def deg-C "°C")
(def deg-F "°F")

(defmulti param->str :type)

(defn parse-params [params]
  (reduce-kv (fn [m k v]
               (assoc m k (param->str v)))
             {} params))

(defmethod param->str "temperature" [param]
  (let [uom @(rf/subscribe [::app-subs/temp-unit])]
    (get param (keyword uom))))

(defmethod param->str "translation" [param]
  (let [ks (map keyword (str/split (:key param) #"\."))]
    (or @(rf/subscribe [::ht-subs/translation ks]) "_")))
