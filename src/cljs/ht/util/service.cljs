(ns ht.util.service
  (:require [clojure.string :as str]
            [cljs.core.async :refer [<! put!]]
            [cljs-http.client :as http]
            [re-frame.core :as rf]
            [ht.config :refer [config]]
            [ht.util.interop :as i])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce api-map (atom {}))

(defn add-to-api-map [api]
  (let [api (->> api   
                 (map (fn [[_ {:keys [root api]}]]
                        (->> api
                             (map (fn [[k uri]]
                                    [k (str root uri)]))
                             (into {}))))
                 (apply merge))]
    (swap! api-map merge api)))

(defonce init-once
  (add-to-api-map
   {:portal {:root (:portal-uri @config)
             :api {:fetch-auth "/auth/token/fetch"
                   :logout ""}}}))

(defn api-uri
  ([api-key]
   (get @api-map api-key))
  ([api-key params]
   (reduce (fn [uri [pkey pval]]
             (str/replace uri (str pkey) (str pval)))
           (get @api-map api-key)
           params)))

(defn fetch-auth [{:keys [evt-success evt-failure]}]
  (go
    (let [{:keys [status body]}
          (<! (http/get (api-uri :fetch-auth)
                        {:headers {"Accept" "application/edn"}
                         :query-params
                         {:timestamp (i/ocall (js/Date.) :valueOf)}}))]
      (if (= status 200)
        ;;success
        (let [{:keys [token claims]} body]
          (rf/dispatch (conj evt-success token claims)))
        ;;failuer
        (do
          (js/console.log [status body])
          (rf/dispatch (conj evt-failure status)))))))

