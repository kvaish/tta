(ns tta.schema.plant
  (:require [tta.schema.sap-plant :as sap-plant]
            [ht.util.schema :as u]))

(def schema
  (let [;; settings
        pinch    {:end-date (u/date-field "endDate")
                  ;; 2D array of indices of pinched tubes
                  :tubes    {:name    "tubes"
                             :parse   js->clj
                             :unparse clj->js}}
        std-temp {:end-date (u/date-field "endDate")
                  :target   "target"
                  :design   "design"}
        settings {:gold-cup-wavelength "goldCupWavelength"
                  :emissivity-type     "emissivityType"
                  :emissivity          "emissivity"
                  :pyrometer-id        "pyrometerId"
                  :pyrometers          {:name   "pyrometers"
                                        :schema :pyrometer
                                        :array? true}
                  :temp-unit           "tempUnit"
                  :target-temp         "targetTemp"
                  :design-temp         "designTemp"
                  :pinch-history       {:name   "pinchHistory"
                                        :schema pinch
                                        :array? true}
                  :std-temp-history    {:name   "stdTempHistory"
                                        :schema std-temp
                                        :array? true}
                  :min-tubes%          "minPctTubesMeasured"
                  :sf-settings         {:name   "sfSettings"
                                        :schema ::sf-settings}
                  :tf-settings         {:name   "tfSettings"
                                        :schema ::tf-settings}
                  :date-modified       (u/date-field "dateModified")
                  :modified-by         "modifiedBy"}

        ;; configuration
        config {:name          "name"
                :version       "version"
                :firing        "firing"
                :sf-config     {:name   "sfConfig"
                                :schema ::sf-config}
                :tf-config     {:name   "tfConfig"
                                :schema ::tf-config}
                :modified-by   "modifiedBy"
                :date-modified (u/date-field "dateModified")}

        history (assoc config
                       :archived-by   "archivedBy"
                       :date-archived (u/date-field "dateArchived"))]

    {:plant (merge (into {}
                         (map (fn [[k f]]
                                [k (if (map? f)
                                     (assoc f :scope #{:api})
                                     {:name f, :scope #{:api}})])
                              (:sap-plant sap-plant/schema)))
                   {:id            u/id-field
                    :client-id     "clientId"
                    :name          "name"
                    :settings      {:name   "settings"
                                    :schema settings}
                    :history       {:name   "history"
                                    :schema history
                                    :array? true}
                    :config        {:name   "configuration"
                                    :schema config}
                    :created-by    "createdBy"
                    :date-created  (u/date-field "dateCreated")
                    :modified-by   "modifiedBy"
                    :date-modified (u/date-field "dateModified")})

     :plant/update-settings ^:api {:last-modified (u/date-field "lastModified")
                                   :settings      {:name   "settings"
                                                   :schema settings}}

     :plant/update-config ^:api {:last-modified (u/date-field "lastModified")
                                 :config        {:name   "configuration"
                                                 :schema config}}

     :plant/push-history (with-meta history {:db true})

     ::tube-settings {:gold-cup-emissivity {:name    "goldCupEmissivity"
                                            :parse   js->clj
                                            :unparse clj->js}
                      :custom-emissivity   {:name    "customEmissivity"
                                            :parse   js->clj
                                            :unparse clj->js}
                      :tube-prefs          {:name    "tubePrefs"
                                            :parse   js->clj
                                            :unparse clj->js}}

     ::tf-settings {:rows {:name "rows"
                           :array? true
                           :schema ::tube-settings}}

     ::sf-settings {:chambers {:name   "chambers"
                               :array? true
                               :schema ::tube-settings}}

     ::tf-config {:wall-names {:name "wallNames"
                               :schema {:north "north"
                                        :east "east"
                                        :west "west"
                                        :south "south"}}
                  :burner-first? "isBurnerFirst"
                  ;; tube/burner rows arranged from west to east
                  ;; and tubes/burners in each rows are north to south
                  ;; sections are arranged north to south
                  :tube-row-count "tubeRowCount"
                  :tube-rows {:name "tubeRows"
                              :array? true
                              :schema {:tube-count "tubeCount"
                                       :start-tube "startTube"
                                       :end-tube "endTube"}}
                  :burner-row-count "burnerRowCount"
                  :burner-rows {:name "burnerRows"
                                :array? true
                                :schema {:burner-count "burnerCount"
                                         :start-burner "startBurner"
                                         :end-burner "endBurner"}}
                  :section-count "sectionCount"
                  :sections {:name "sections"
                             :array? true
                             :schema {:tube-count "tubeCount"
                                      :burner-count "burnerCount"}}
                  :measure-levels {:name "measureLevels"
                                   :schema {:top? "hasTop"
                                            :bottom? "hasBottom"
                                            :middle? "hasMiddle"}}}

     ::sf-config {:chambers         {:name   "chambers"
                                     :schema ::chamber
                                     :array? true}
                  :placement-of-WHS "placementOfWHS"
                  :dual-nozzle?     "dualFuelNozzle"}

     ::chamber {:name                 "name"
                :side-names           {:name    "sideNames"
                                       :parse   js->clj
                                       :unparse clj->js}
                :section-count        "sectionCount"
                :peep-door-tube-count {:name    "peepDoorTubeCount"
                                       :parse   js->clj
                                       :unparse clj->js}
                :peep-door-count      "peepDoorCount"
                :tube-count           "tubeCount"
                :end-tube             "endTube"
                :start-tube           "startTube"
                :burner-count-per-row "burnerCountPerRow"
                :start-burner         "startBurner"
                :end-burner           "endBurner"
                :burner-row-count     "burnerRowCount"}}))
