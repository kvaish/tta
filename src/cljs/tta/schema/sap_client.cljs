(ns tta.schema.sap-client)

(def schema
  {:db/sap-client {:id         "id"    ;; GUID string
                   :sap-id     "sapId" ;; user_code from client database
                   :name       "name"
                   :short-name "shortName"
                   :address    {:name "address"
                                :schema
                                {:address     "address"
                                 :po-box-name "poBoxName"
                                 :po-box      "poBox"
                                 :po-zip-code "poZipCode"
                                 :po-city     "poCity"
                                 :zip-code    "zipCode"}}
                   :city       "city"
                   :state      "state"
                   :country    "country"
                   :location   "location"}})
