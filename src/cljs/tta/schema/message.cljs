(ns tta.schema.message
  (:require [ht.util.schema :as u]))

(def schema
  {:message {:id                u/id-field
             :date              (u/date-field "date")
             :client-id         "clientId"
             :plant-id          "plantId"
             :dataset-id        "datasetId"
             :level             "level"
             :template-key      "templateKey"
             :parameters        "parameters"
             :acknowledged-by   "acknowledgedBy"
             :date-acknowledged (u/date-field "dateAcknowledged")}})
