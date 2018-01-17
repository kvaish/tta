(ns tta.schema.plant
  (:require [tta.schema.sap-plant :as sap-plant]
            [ht.util.interop :as u]))

(defn- add-prefix-1 [prefix schema]
  (reduce-kv (fn [m k v]
               (assoc m k
                      (if (map? v)
                        (update v :name #(str prefix "." %))
                        (str prefix "." v))))
             {} schema))

(def schema
  (let [;; settings
        pinch    {:end-date "endDate"
                  ;; 2D array of indices of pinched tubes
                  :tubes    {:name    "tubes"
                             :parse   js->clj
                             :unparse clj->js}}
        std-temp {:end-date "endDate"
                  :target   "target"
                  :design   "design"}
        settings {:emissivity-type  "emissivityType"
                  :emissivity       "emissivity"
                  :pyrometer-id     "pyrometerId"
                  :pyrometers       {:name   "pyrometers"
                                     :schema :pyrometer
                                     :array? true}
                  :temp-unit        "tempUnit"
                  :target-temp      "targetTemp"
                  :design-temp      "designTemp"
                  :pinch-history    {:name   "pinchHistory"
                                     :schema pinch
                                     :array? true}
                  :std-temp-history {:name   "stdTempHistory"
                                     :schema std-temp
                                     :array? true}
                  :min-tubes%       "minPctTubesMeasured"
                  :sf-settings      {:name   "sfSettings"
                                     :schema ::sf-settings}
                  :tf-settings      {:name   "tfSettings"
                                     :schema ::tf-settings}
                  :date-modified    "dateModified"
                  :modified-by      "modifiedBy"}

        update-settings ;; schema for receiving from api
        (-> settings
            (dissoc :pinch-history :std-temp-history)
            (assoc :archive-pinch {:name   "archivePinch"
                                   :schema pinch}
                   :archive-std-temp {:name   "archiveStdTemp"
                                      :schema std-temp}))
        db-settings     ;; schema for sending to db
        (as-> settings $
          (assoc $ :date-modified "dateModified")
          (assoc-in $ [:pyrometers :schema] :db/pyrometer)
          (assoc-in $ [:pinch-history :schema :end-date] "endDate")
          (assoc-in $ [:std-temp-history :schema :end-date] "endDate"))

        ;; configuration
        config {:name          "name"
                :version       "version"
                :firing        "firing"
                :sf-config     {:name   "sfConfig"
                                :schema ::sf-config}
                :tf-config     {:name   "tfConfig"
                                :schema ::tf-config}
                :modified-by   "modifiedBy"
                :date-modified "dateModified"
                :date-archived "dateArchived"
                :archived-by   "archivedBy"}
        db-history (assoc config
                          :date-modified "dateModified"
                          :date-archived "dateArchived")
        db-config (dissoc db-history :date-archived :archived-by)

        ;; plant
        plant {:id            "id"
               :client-id     "clientId"
               :name          "name"
               :settings      {:name   "settings"
                               :schema settings}
               :history       {:name   "history"
                               :schema config
                               :array? true}
               :config        {:name   "configuration"
                               :schema config}
               :created-by    "createdBy"
               :date-created  "dateCreated"
               :modified-by   "modifiedBy"
               :date-modified "dateModified"}

        db-plant (-> plant
                     (assoc :date-created  "dateCreated"
                            :date-modified "dateModified")
                     (assoc-in [:settings :schema] db-settings)
                     (assoc-in [:history :schema] db-history)
                     (assoc-in [:config :schema] db-config))
        ]


    {:plant (merge plant
                   (:db/sap-plant sap-plant/schema))

     :db/plant          db-plant
     :plant/settings    update-settings
     :db.plant/settings (add-prefix-1 "settings" db-settings)
     :plant/config      {:archive {:name   "archive"
                                   :schema config}
                         :update  {:name   "update"
                                   :schema config}}
     :db.plant/config   (add-prefix-1 "configuration" db-config)
     :db.plant/history  db-history

     ::tf-settings {}

     ::sf-settings {:gold-cup-emissivity {:name    "goldCupEmissivity"
                                          :parse   js->clj
                                          :unparse clj->js}
                    :custom-emissivity   {:name    "customEmissivity"
                                          :parse   js->clj
                                          :unparse clj->js}
                    :tube-prefs          {:name    "tubePrefs"
                                          :parse   js->clj
                                          :unparse clj->js}}

     ::tf-config {}

     ::sf-config {:chambers         {:name   "chambers"
                                     :schema ::chamber
                                     :array? true}
                  :placement-of-WHS "placementOfWHS"
                  :dual-nozzle?     "dualFuelNozzle"}

     ::chamber {:name                 "name"
                :side-names           {:name    "sideName"
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
