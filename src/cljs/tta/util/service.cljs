(ns tta.util.service
  (:require [cljs.core.async :refer [<! put!]]
            [cljs-http.client :as http]
            [re-frame.core :as rf]
            [ht.config :refer [config]]
            [ht.util.service :refer [add-to-api-map run]]
            [tta.schema.model :refer [from-api to-api]]
            [ht.util.schema :as u])
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
          :fetch-client-plants "/api/client/:client-id/plant"
          :create-client "/api/client"
          :update-client "/api/client/:client-id"
          :create-plant "/api/client/:client-id/plant"
          :update-plant-config "/api/client/:client-id/plant/:plant-id/config"
          :update-plant-settings "/api/client/:client-id/plant/:plant-id/settings"
          :search-datasets "/api/client/:client-id/plant/:plant-id/dataset"
          :fetch-latest-dataset "/api/client/:client-id/plant/:plant-id/latest-dataset"
          :fetch-dataset "/api/client/:client-id/plant/:plant-id/dataset/:dataset-id"
          :create-dataset "/api/client/:client-id/plant/:plant-id/dataset"
          :update-dataset "/api/client/:client-id/plant/:plant-id/dataset/:dataset-id"
          :delete-dataset "/api/client/:client-id/plant/:plant-id/dataset/:dataset-id"}}))


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

(defn save-client [{:keys [client new?
                           evt-success evt-failure]}]
  (run (merge {:data {:json-params (to-api :client client)}
               :evt-failure evt-failure}
              (if new?
                {:method http/post
                 :api-key :create-client
                 :on-success (dispatch-one evt-success :res/create)}
                {:method http/put
                 :api-key :update-client
                 :api-params {:client-id (:id client)}
                 :on-success (dispatch-one evt-success :res/update)}))))

(defn create-plant [{:keys [plant
                            client-id
                            evt-success evt-failure]}]
  (run {:method http/post
        :api-key :create-plant
        :api-params {:client-id client-id}
        :data {:json-params (to-api :plant plant)}
        :on-success (dispatch-one evt-success :res/create)
        :evt-failure evt-failure}))

(defn update-plant-config [{:keys [update-config
                                   client-id plant-id
                                   evt-success evt-failure]}]
  (run {:method http/put
        :api-key :update-plant-config
        :api-params {:client-id client-id, :plant-id plant-id}
        :data {:json-params (to-api :plant/update-config update-config)}
        :on-success (dispatch-one evt-success :res/update)
        :evt-failure evt-failure}))

(defn update-plant-settings [{:keys [update-settings
                                     client-id plant-id
                                     evt-success evt-failure]}]
  (run {:method http/put
        :api-key :update-plant-settings
        :api-params {:client-id client-id, :plant-id plant-id}
        :data {:json-params (to-api :plant/update-settings update-settings)}
        :on-success (dispatch-one evt-success :res/update)
        :evt-failure evt-failure}))

(defn search-datasets [{:keys [client-id plant-id query
                               evt-success evt-failure]}]
  (run {:method      http/get
        :api-key     :search-datasets
        :api-params  {:client-id client-id, :plant-id plant-id}
        :data        {:query-params (to-api :dataset/query query)}
        :on-success  (dispatch-many evt-success :dataset)
        :evt-failure evt-failure}))

(defn fetch-latest-dataset [{:keys [client-id plant-id
                                    evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-latest-dataset
        :api-params {:client-id client-id, :plant-id plant-id }
        :on-success (dispatch-one evt-success :dataset)
        :evt-failure evt-failure}))

(defn fetch-dataset [{:keys [client-id plant-id dataset-id
                             evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-dataset
        :api-params {:client-id client-id
                     :plant-id plant-id
                     :dataset-id dataset-id}
        :on-success (dispatch-one evt-success :dataset)
        :evt-failure evt-failure}))

(defn save-dataset [{:keys [new? dataset
                              evt-success evt-failure]}]
  (let [api-params (select-keys dataset [:client-id :plant-id])]
    (run (merge {:data {:json-params (to-api :dataset dataset)}
                 :evt-failure evt-failure}
                (if new?
                  {:method http/post
                   :api-key :create-dataset
                   :api-params api-params
                   :on-success (dispatch-one evt-success :res/create)}
                  {:method http/put
                   :api-key :update-dataset
                   :api-params (assoc api-params :dataset-id (:id dataset))
                   :on-success (dispatch-one evt-success :res/update)})))))

(defn delete-dataset [{:keys [client-id plant-id dataset-id
                              evt-success evt-failure]}]
  (run {:method http/delete
        :api-key :delete-dataset
        :api-params {:client-id client-id
                     :plant-id plant-id
                     :dataset-id dataset-id}
        :on-success (dispatch-one evt-success :res/delete)
        :evt-failure evt-failure}))
