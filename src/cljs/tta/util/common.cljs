(ns tta.util.common
  (:require [re-frame.core :as rf]
            [tta.config :refer [config]]
            [tta.util.interop :as i]
            [clojure.string :as str]))

(defn get-window-size []
  ;; (js/console.log js/window.innerHeight)
  {:width (i/oget js/window :innerWidth)
   :height (i/oget js/window :innerHeight)})

(defn translate [key-v default]
  (or @(rf/subscribe (conj [:tta.app.subs/translate] key-v))
      default))

(defn get-storage-js
  "get from localStorage as a js Object"
  ([key]
   (get-storage-js key false))
  ([key common?]
   (let [key (if common? key
                 (str (:app-id @config) "_" key))]
     (-> (i/oget js/localStorage key)
         i/json-parse))))

(defn get-storage
  "get from localStorage as a clojure map"
  ([key]
   (get-storage key false))
  ([key common?]
   (js->clj (get-storage-js key common?) :keywordize-keys true)))

(defn set-storage-js
  "save to localStorage a js object"
  ([key value]
   (set-storage-js key value false))
  ([key value common?]
   (let [key (if common? key
                 (str (:app-id @config) "_" key))]
     (->> (i/json-str value)
          (i/oset js/localStorage key)))))

(defn set-storage
  "save to localStorage a clojure map"
  ([key value]
   (set-storage key value false))
  ([key value common?]
   (set-storage-js key (clj->js value) common?)))
