(ns tta.schema.message)

(def schema
  (let [message {:id                "id"
                 :date              "date"
                 :client-id         "clientId"
                 :plant-id          "plantId"
                 :dataset-id        "datasetId"
                 :level             "level"
                 :template-key      "templateKey"
                 :parameters        "parameters"
                 :acknowledged-by   "acknowledgedBy"
                 :date-acknowledged "dateAcknowledged"}

        db-message (assoc message
                          :date "date"
                          :date-acknowledged "dateAcknowledged")]

    {:message    message
     :db/message db-message}))
