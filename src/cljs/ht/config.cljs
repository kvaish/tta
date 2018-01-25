(ns ht.config
  (:require [ht.util.interop :as i]))

(def key-map {:app-id      :appId
              :build-id    :buildId
              :portal-uri  :portalUri
              :service-uri :serviceUri
              :languages   :languages})

(defonce config (atom {}))

(defn load-config-js [js-config]
  (->
   (reduce (fn [m [k js-k]]
             (assoc m k (i/oget js-config js-k)))
           {} key-map)
   (update :languages js->clj :keywordize-keys true)))

(defn init []
  (swap! config merge (load-config-js js/htAppConfig)))
