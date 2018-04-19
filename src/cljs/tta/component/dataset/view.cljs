;; view elements component dataset
(ns tta.component.dataset.view
  (:require [reagent.core :as r]
            [reagent.format :refer [format]]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [ht.util.interop :as i]
            [ht.util.common :as u]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.icon :as ic]
            [tta.app.view :as app-view]
            [tta.app.comp :as app-comp]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]
            [tta.dialog.dataset-settings.view :refer [dataset-settings]]
            [tta.component.dataset.twt-entry :refer [twt-entry]]
            [tta.component.dataset.twt-graph :refer [twt-graph]]
            [tta.component.dataset.burner-entry :refer [burner-entry]]
            [tta.component.dataset.overall-graph :refer [overall-graph]]
            [tta.component.dataset.burner-status :refer [burner-status]]))

;; (def date-formatter (tf/formatter "yyyy-MM-dd"))
;; (defn format-date [date] (tf/unparse date-formatter (t/date-time date)))



(defn show-error? []
  @(rf/subscribe [::subs/show-error?]))

;; visible only when creating draft
;; disable when can't save
(defn action-save []
  [app-comp/button
   {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
    :icon ic/save
    :on-click #(rf/dispatch [::event/save-draft])
    :label (translate [:dataset :action :save] "Save")}])

;;dataset settings visible only in edit mode
(defn action-settings [data]
  [app-comp/button
   {:disabled? false
    :icon ic/menu
    :label (translate [:dataset :action :settings] "Settings")
    :on-click #(rf/dispatch [:tta.dialog.dataset-settings.event/open
                             {:dataset data}])}])

;;discard draft, visible in edit mode and not published yet
(defn action-clear-draft []
  [app-comp/button
   {:icon ic/delete
    :label (translate [:dataset :action :clear] "Clear")
    :on-click #(rf/dispatch [::event/clear-draft])}])

;;report download, visible in read mode
(defn action-report []
  [app-comp/button
   {:disabled? false
    :icon ic/report
    :label (translate [:dataset :action :report] "Report")
    :on-click #(rf/dispatch [::event/report])}])

;;datasheet download, visible in read mode
(defn action-datasheet []
  [app-comp/button
   {:disabled? false
    :icon ic/datasheet
    :label (translate [:dataset :action :excel] "Datasheet")
    :on-click #(rf/dispatch [::event/datasheet])}])

;;upload dataset if dirty and valid
(defn action-upload []
  [app-comp/button
   {:disabled? (not @(rf/subscribe [::subs/can-upload?]))
    :icon ic/upload
    :on-click #(rf/dispatch [::event/upload])
    :label (translate [:dataset :action :upload] "Upload")}])

;;delete dataset from database
(defn action-delete []
  [app-comp/button
   {:icon ic/delete
    :on-click #(rf/dispatch [::event/delete])
    :label (translate [:dataset :action :delete] "Delete")}])

;;disabled when dataset and plant reformer version mismatched
(defn action-selector []
  (let [plant-version (get-in @(rf/subscribe [::app-subs/plant])
                              [:config :version])
        reformer-version  @(rf/subscribe [::subs/reformer-version])
        mode @(rf/subscribe [::subs/mode])
        mode-opts @(rf/subscribe [::subs/mode-opts])
        sel-mode-opt (some #(if (= (:id %) mode) %) mode-opts)]
    [app-comp/selector {:item-width 70
                        :disabled? (not (= plant-version
                                           reformer-version))
                        :label-fn :label
                        :options mode-opts
                        :selected sel-mode-opt
                        :on-select #(rf/dispatch [::event/set-mode
                                                  (:id %)])}]) )

;; dataset list, visible in read mode only
(defn action-dataset-list []
  [app-comp/button
   {:disabled? true
    :icon ic/dataset
    :on-click #(js/console.log "list dataset: not implemented yet")
    :label (translate [:dataset :action :dataset-list] "Datasets")}])

(defn action-publish-gold-cup []
  [app-comp/button
   {:disabled? true
    :icon ic/upload
    :on-click #(js/console.log "publish goldcup: not implemented yet")
    :label (translate [:dataset :action :publish-gold-cup] "Gold Cup")}])

(defn get-actions []
  (let [data @(rf/subscribe [::subs/data])
        {:keys [draft? gold-cup?]} data
        mode @(rf/subscribe [::subs/mode])
        topsoe? @(rf/subscribe [::ht-subs/topsoe?])
        can-edit? @(rf/subscribe [::subs/can-edit?])
        can-delete? @(rf/subscribe [::subs/can-delete?])]
    (if (= mode :edit)
      (list (if (or draft? can-edit?) (action-settings data))
            (if draft? (action-clear-draft))
            (if draft? (action-save))
            (if (or draft? can-edit?) (action-upload))
            (action-selector))
      (list (action-datasheet)
            (action-report)
            (if (and topsoe? gold-cup?) (action-publish-gold-cup))
            (if (or draft? can-edit?) (action-upload))
            (if (and (not draft?) can-delete?) (action-delete))
            (action-dataset-list)
            (action-selector)))))

(defn body [{:keys [width height]}]
  (let [area-opts @(rf/subscribe [::subs/area-opts])
        selected-area @(rf/subscribe [::subs/selected-area])
        level-opts @(rf/subscribe [::subs/level-opts])
        selected-level @(rf/subscribe [::subs/selected-level])
        firing @(rf/subscribe [::subs/firing])]
    [app-view/tab-layout
     {:top-tabs {:selected (or selected-area 0)
                 :on-select #(rf/dispatch [::event/select-area %])
                 :labels (mapv :label area-opts)}
      :bottom-tabs {:selected (or selected-level 0)
                    :on-select #(rf/dispatch [::event/select-level %])
                    :labels (mapv :label level-opts)}
      :width width, :height height
      :content
      (fn [{:keys [width height selected]}]
        (let [mode @(rf/subscribe [::subs/mode])
              [sel-top sel-bottom] selected
              view-size {:width width
                         :height height}]
          (cond
            ;; topfired
            (= "top" firing)
            (cond
              (= :edit mode) ;; twt-entry, burner, gold-cup
              (case sel-top
                0 ;; twt-entry
                [twt-entry {:level sel-bottom
                            :view-size view-size}]
                1 ;; burner-entry
                [burner-entry {:view-size view-size}]
                2 ;; gold-cup-entry
                [:div {:style view-size} "gold-cup entry topfired"])

              (= :view mode)
              (case sel-top
                0 ;; overall
                [overall-graph {:level sel-bottom
                                :view-size view-size}]
                1 ;; twt-graph
                [twt-graph {:level sel-bottom
                            :view-size view-size}]
                2 ;; burner-status
                [burner-status {:view-size view-size}]
                3 ;; gold-cup-view
                [:div {:style view-size} "gold-cup view topfired"]))

            ;; sidefired
            (= "side" firing)
            (cond
              (= :edit mode)
              (case sel-top
                0 ;; twt-entry
                [twt-entry {:level sel-bottom
                            :view-size view-size}]
                1 ;; burner-entry
                [burner-entry {:view-size view-size}]
                2 ;; gold-cup-entry
                [:div {:style view-size} "gold-cup entry sidefired"])

              (= :view mode)
              (case sel-top
                0 ;; twt-graph
                [twt-graph {:view-size view-size}]
                1 ;; burner-status
                [burner-status {:view-size view-size}]
                2 ;; gold-cup-view
                [:div {:style view-size} "gold-cup view sidefired"])))))}]))

;; dataset: date | time
;; last saved: date | time (hide when nil)

;; buttons array:
;; mode :read - upload (disable if not dirty), excel report, pdf report
;;              publish goldcup (if goldcup? and internal? user)
;;              (disable if not 100% or not uploaded)
;; mode :edit - settings, save, upload
;; mode selector : disabled when reformer version not current

(defn dataset [props]
  (let [last-saved @(rf/subscribe [::subs/last-saved])
        data-date @(rf/subscribe [::subs/data-date])]
    [:div
     [app-view/layout-main
      (str (translate [:dataset :title :text] "Dataset")
           ": "
           (let [{:keys [year month day hour minute]}
                 (u/to-date-time-map data-date)]
             (format "%4d-%02d-%02d | %02d:%02d"
                     year month day hour minute)))
      (str (translate [:dataset :sub-title :text] "Last saved")
           ": "
           (if last-saved
             (let [{:keys [year month day hour minute second]}
                   (u/to-date-time-map last-saved)]
               (format "%4d-%02d-%02d | %02d:%02d:%02d"
                       year month day hour minute second))))

      (get-actions)
      body]

     ;;dataset settings dialog
     (if @(rf/subscribe [:tta.dialog.dataset-settings.subs/open?])
       [dataset-settings])]))
