(ns tta.util.service
  (:require [cljs.core.async :refer [<! put!]]
            [cljs-http.client :as http]
            [re-frame.core :as rf]
            [ht.config :refer [config]]
            [ht.util.service :refer [add-to-api-map run]]
            [tta.schema.model :refer [from-api to-api]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn init []
  (add-to-api-map
   {:root (:service-uri @config)
    :api {:fetch-user "/api/user/:user-id"
          :create-user "/api/user"
          :update-user "/api/user/:user-id"
          :fetch-client-search-options "/api/client/search-options"
          :fetch-client "/api/client/:client-id"
          :search-clients "/api/client"
          :fetch-plant "/api/client/:client-id/plant/:plant-id"
          :fetch-client-plants "/api/client/:client-id/plant"}}))

(defn dispatch-one [evt entity-key]
  #(rf/dispatch (conj evt (from-api entity-key (:result %)))))

(defn dispatch-many [evt entity-key]
  (fn [res]
    (rf/dispatch (conj evt (map #(from-api entity-key %)
                                (:result res))))))

(defn fetch-user [{:keys [user-id evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-user
        :api-params {:user-id user-id}
        :on-success (dispatch-one evt-success :user)
        :evt-failure evt-failure}))

(defn save-user [{:keys [user new? evt-success evt-failure]}]
  (run (merge {:data {:json-params (to-api :user user)}
               :evt-failure evt-failure}
              (if new?
                {:method http/post
                 :api-key :create-user
                 :on-success (dispatch-one evt-success :res/create)}
                {:method http/put
                 :api-key :update-user
                 :api-params {:user-id (:id user)}
                 :on-success (dispatch-one evt-success :res/update)}))))

(defn fetch-search-options [{:keys [evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-client-search-options
        :on-success (dispatch-one evt-success :client/search-options)
        :evt-failure evt-failure}))

(defn fetch-client [{:keys [client-id evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-client
        :api-params {:client-id client-id}
        :on-success (dispatch-one evt-success :client)
        :evt-failure evt-failure}))

(defn search-clients [{:keys [query evt-success evt-failure]}]
  (run {:method http/get
        :api-key :search-clients
        :data {:query-params (to-api :client/query query)}
        :on-success (dispatch-many evt-success :sap-client)
        :evt-failure evt-failure}))

(defn fetch-plant [{:keys [client-id plant-id evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-plant
        :api-params {:client-id client-id, :plant-id plant-id}
        :on-success (dispatch-one evt-success :plant)
        :evt-failure evt-failure}))

(defn fetch-client-plants [{:keys [client-id evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-client-plants
        :api-params {:client-id client-id}
        :on-success (dispatch-many evt-success :sap-plant)
        :evt-failure evt-failure}))
