(ns tta.schema.dataset
  (:require [ht.util.schema :as u]))

(def schema
  {:dataset {:id               u/id-field
             :client-id        "clientId"
             :plant-id         "plantId"
             :data-date        (u/date-field "dataDate")
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
             :role-type        "roleType" ;; TODO: remove later :PKPA
             :shift            "shift"
             :comment          "comment"
             :operator         "operator"
             :created-by       "createdBy"
             :date-created     (u/date-field "dateCreated")
             :modified-by      "modifiedBy"
             :date-modified    (u/date-field "dateModified")}

   :dataset/query ^:api {:utc-start {:name  "utcStart"
                                     :parse u/parse-date}
                         :utc-end   {:name  "utcEnd"
                                     :parse u/parse-date}}

   ::summary {:tubes%       "pctTubesMeasured"
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
           :burners    {:name      "burners"
                        :schema    ::burner
                        :array?    true
                        :array-dim 2}
           :wall-temps {:name   "wallTemps"
                        :schema ::wall-temps
                        :array? true}}

   ::tube {:gold-cup-temp         "goldCupTemp"
           :raw-temp              "rawTemp"
           :corrected-temp        "correctedTemp"
           :emissivity            "emissivity"
           :emissivity-calculated "emissivityCalculated"
           :emissivity-override   "emissivityOverride"
           :pinched?              "isPinched"}

   ::burner {:state "state"}

   ::wall-temps {:avg   "avg"
                 :temps {:name    "temps"
                         :parse   js->clj
                         :unparse clj->js}}})
