(ns tta.util.auth
  (:require [ht.util.auth :as ht-auth]
            [tta.info :as info]))

(defn allow-app? [claims]
  (and
   (ht-auth/allow-feature? claims :standard)
   (ht-auth/allow-operation? claims :view info/operations)))

(defn allow-settings? [claims]
  (ht-auth/allow-operation? claims :modifyPlantSettings info/operations))

(defn allow-goldcup? [claims]
  (and (:topsoe? claims)
       (ht-auth/allow-operation? claims :editDataset info/operations)))

(defn allow-config-history? [claims]
  (:topsoe? claims))

(defn allow-config? [claims]
  (and (:topsoe? claims)
       (ht-auth/allow-operation? claims :configureReformer info/operations)))

(defn allow-data-entry? [claims]
  (or (ht-auth/allow-operation? claims :editDataset info/operations)
      (ht-auth/allow-operation? claims :createDataset info/operations)))

(defn allow-import-logger? [claims]
  (and (ht-auth/allow-feature? claims :importDataset)
       (allow-data-entry? claims)))

(defn allow-root-content?
  [claims content-id]
  ;;TODO: apply access-rules
  (case content-id
    :home           true
    :dataset        true
    :trendline      true
    :settings       (allow-settings? claims)
    :gold-cup       (allow-goldcup? claims)
    :config-history (allow-config-history? claims)
    :config         (allow-config? claims)
    :logs           true
    ;; buttons
    :data-entry     (allow-data-entry? claims)
    :import-logger  (allow-import-logger? claims)
    :print-logsheet true
    false))


(defn allow-edit-dataset? [claims]
  (ht-auth/allow-operation? claims :editDataset info/operations))

(defn allow-delete-dataset? [claims]
  (ht-auth/allow-operation? claims :deleteDataset info/operations))

(defn allow-export? [claims]
  (ht-auth/allow-operation? claims :export info/operations))
