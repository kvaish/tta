(ns ht.util.common
  (:require [clojure.string :as str]
            [ht.config :refer [config]]
            [ht.util.interop :as i]))

(defn pick-one
  "return the first one in collection for which pred is true. if
none matched, returns nil."
  [pred coll]
  (first (filter pred coll)))

(defn get-window-size []
  ;; (js/console.log js/window.innerHeight)
  {:width (i/oget js/window :innerWidth)
   :height (i/oget js/window :innerHeight)})

(defn- key->str [key]
  (cond
      (keyword? key) (name key)
      (string? key) key
      :error (throw "invalid key! must be a string or keyword.")))

(defn- get-storage-js
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
   (js->clj (get-storage-js (key->str key) common?)
            :keywordize-keys true)))

(defn- set-storage-js
  "save to localStorage a js object"
  ([key value]
   (set-storage-js key value false))
  ([key value common?]
   (let [key (if common? key
                 (str (:app-id @config) "_" key))]
     (->> (i/json-str value)
          (i/oset js/localStorage key)))))

(defn set-storage
  "save to localStorage a clojure map.  
*key* should be a string or keyword"
  ([key value]
   (set-storage key value false))
  ([key value common?]
   (set-storage-js (key->str key) (clj->js value) common?)))
