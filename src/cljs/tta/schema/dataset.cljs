(ns tta.schema.dataset)

(def schema
  (let [dataset {:id               "id"
                 :client-id        "clientId"
                 :plant-id         "plantId"
                 :data-date        "dataDate"
                 :topsoe?          "isTopsoeInternal"
                 :reformer-version "reformerVersion"
                 :summary          {:name   "summary"
                                    :schema ::summary}
                 :pyrometer        {:name   "pyrometer"
                                    :schema :pyrometer}
                 :emissivity-type  "emissivityType"
                 :emissivity       "emissivity"
                 :side-fired       {:name   "sideFired"
                                    :schema ::side-fired}
                 :top-fired        {:name   "topFired"
                                    :schema ::top-fired}
                 :shift            "shift"
                 :comment          "comment"
                 :operator         "operator"
                 :created-by       "createdBy"
                 :date-created     "dateCreated"
                 :modified-by      "modifiedBy"
                 :date-modified    "dateModified"}

        db-dataset (-> (assoc dataset
                              :data-date "dataDate"
                              :date-created "dateCreated"
                              :date-modified "dateModified")
                       (assoc-in [:pyrometer :schema] :db/pyrometer))]

    {:dataset    dataset
     :db/dataset db-dataset

    #_ :dataset/query #_{:utc-start {:name  "utcStart"
                                 :parse u/parse-date}
                     :utc-end   {:name  "utcEnd"
                                 :parse u/parse-date}}

     ::summary {:tubes%       "pctTubesMeasured "
                :gold-cup%    "pctGoldCupMeasured"
                :min-temp     "minTemp"
                :avg-temp     "avgTemp"
                :max-temp     "maxTemp"
                :min-raw-temp "minRawTemp"
                :avg-raw-temp "avgRawTemp"
                :max-raw-temp "maxRawTemp"
                :sub-summary  {:name   "subSummary"
                               :schema ::summary
                               :array? true}}

     ::top-fired {}

     ::side-fired {:chambers {:name   "chambers"
                              :schema ::chamber
                              :array? true}}

     ::chamber {:sides {:name   "sides"
                        :schema ::side
                        :array? true}}

     ::side {:tubes      {:name   "tubes"
                          :schema ::tube
                          :array? true}
             :burners    {:name   "burners"
                          :schema ::burner
                          :array? true}
             :wall-temps {:name   "wallTemps"
                          :schema ::wall-temps
                          :array? true}}

     ::tube {:gold-cup-temp         "goldCupTemp"
             :raw-temp              "rawTemp"
             :corrected-temp        "correctedTemp"
             :emissivity            "emissivity"
             :emissivity-calculated "emissivityCalculated"
             :emissivity-override   "emissivityOverride"}

     ::burner {:state "state"}

     ::wall-temps {:avg   "avg"
                   :temps {:name    "temps"
                           :parse   js->clj
                           :unparse clj->js}}}))
