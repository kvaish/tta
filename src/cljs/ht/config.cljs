(ns ht.config
  (:require [ht.util.interop :as i]))

(def debug?
  ^boolean goog.DEBUG)

(def key-map {:app-id :appId
              :portal-uri :portalUri
              :service-uri :serviceUri
              :languages :languages})

(defonce config (atom {}))

(defn parse-languages-js [js-languages]
  (->> (js->clj js-languages :keywordize-keys true)
       (mapv (fn [{:keys [code flag name]}]
               {:id (keyword code)
                :name name}))))

(defn load-config-js [js-config]
  (as-> js-config $
   (reduce (fn [m [k js-k]]
             (assoc m k (i/oget $ js-k)))
           {} key-map)
   (assoc $ :language-options (parse-languages-js (:languages $)))))

(defn init []
  (swap! config merge (load-config-js js/htAppConfig)))
