;; view elements component dataset
(ns tta.component.dataset.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]
            [tta.dialog.dataset-settings.view :refer [dataset-settings]]))

;; dataset: date | time
;; last saved: date | time (hide when nil)

;; buttons array:
;; mode :read - upload (disable if not dirty), excel report, pdf report
;;              publish goldcup (if goldcup? and internal? user), (disable if not 100% or not uploaded)
;; mode :edit - settings, save, upload
;; mode selector : disabled when reformer version not current

(defn dataset [props]
  
  [:div ;; (use-style style/dataset)
   "dataset"
   
   ;;dataset settings dialog
   (if @(rf/subscribe [:tta.dialog.dataset-settings.subs/open?])
     [dataset-settings])])
