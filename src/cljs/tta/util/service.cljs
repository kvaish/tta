(ns tta.util.service
  (:require [clojure.string :as str]
            [cljs.core.async :refer [<! put!]]
            [cljs-http.client :as http]
            [re-frame.core :as rf]
            [ht.config :refer [config]]
            [ht.util.interop :as i]
            [ht.app.subs :as ht-subs]
            [tta.entity :as e]
            [ht.util.service :refer [add-to-api-map api-uri]])
  (:require-macros [cljs.core.async.macros :refer [go]]))



(defonce init-once
  (add-to-api-map
   {:service {:root (:service-uri @config)
              :api {:fetch-user-settings "/api/user/"}}}))


(defn fetch-user-settings [{:keys [user-id evt-success evt-failure]}]
  (go
    (let [token @(rf/subscribe [::ht-subs/auth-token])
          {:keys [status body]} 
          (<! (http/get  (str (:service-uri @config)
                              (api-uri :fetch-user-settings) user-id )
                         {:with-credentials? false
                          :headers { "Accept" "application/json"
                                    "authorization" (str "Token " token)}
                          #_:query-params
                          #_{:timestamp (i/ocall (js/Date.) :valueOf)}}))]
      (if (= status 200)
        ;;success
        (let [{:keys [err result]} body]
          (rf/dispatch (conj evt-success result)))
        ;;failuer
        (do
          (js/console.log [status body])
          (rf/dispatch (conj evt-failure)))))))

(defn update-user-settings [{:keys [user-id data evt-success evt-failure]}]
  (go
    (let [token @(rf/subscribe [::ht-subs/auth-token])
          {:keys [status body]} 
          (<! (http/put (str (:service-uri @config)
                             (api-uri :fetch-user-settings) user-id)
                        {:with-credentials? false
                         :headers { "Accept" "application/json"
                                   "authorization" (str "Token " token)}
                         :json-params (js->clj (e/to-js :user data))}))]
      (if (= status 200)
        ;;success
        (let [{:keys [err result]} body]
          (rf/dispatch (conj evt-success result)))
        ;;failuer
        (do
          (js/console.log [status body])
          (rf/dispatch (conj evt-failure)))))))
