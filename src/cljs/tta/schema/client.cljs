(ns tta.schema.client
  (:require [ht.util.interop :as u]
            [tta.schema.sap-client :as sap-client]))

(def schema
  (let [client {:id     "id"
                :name   "name"
                :plants {:name   "plants"
                         :schema ::plant
                         :array? true}

                :date-created  "dateCreated"
                :created-by    "createdBy"
                :modified-by   "modifiedBy"
                :date-modified "dateModified"}

        db-client (assoc client
                         :date-created "dateCreated"
                         :date-modified "dateModified")]

    {:client (merge client
                    (:db/sap-client sap-client/schema))

     :db/client db-client

     :client/search-options {:country {:name   "country"
                                       :array? true}}

     ::plant {:id       "id"
              :name     "name"
              :capacity "capacity"}}))
