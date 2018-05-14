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
            [ht.style :refer [color-hex]]
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
            [tta.component.dataset.burner-status :refer [burner-status]]
            [tta.component.dataset-selector.view :refer [dataset-selector]]))

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
   {;:disabled? false
    :icon ic/gear
    :label (translate [:dataset :action :settings] "Settings")
    :on-click #(rf/dispatch [:tta.dialog.dataset-settings.event/open
                             {:dataset data}])}])

;;discard draft, visible in edit mode and not published yet
(defn action-reset-draft []
  [app-comp/button
   {:icon ic/reset
    :label (translate [:dataset :action :reset] "Reset")
    :on-click #(rf/dispatch [::event/reset-draft])}])

;;report download, visible in read mode
(defn action-report []
  [app-comp/button
   {:disabled? (not @(rf/subscribe [::subs/can-export?]))
    :icon ic/report
    :label (translate [:dataset :action :report] "Report")
    :on-click #(rf/dispatch [::event/report])}])

;;datasheet download, visible in read mode
(defn action-datasheet []
  [app-comp/button
   {:disabled? (not @(rf/subscribe [::subs/can-export?]))
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
    :on-click #(rf/dispatch [::event/delete-dataset])
    :label (translate [:dataset :action :delete] "Delete")}])

;;disabled when dataset and plant reformer version mismatched
(defn action-select-mode []
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
  [dataset-selector {:selected-id (:id @(rf/subscribe [::subs/data]))
                     :warn-on-selection-change? @(rf/subscribe [::subs/warn-on-close?])}])

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
        can-delete? @(rf/subscribe [::subs/can-delete?])
        dirty? @(rf/subscribe [::subs/dirty?])]
    (if (= mode :edit)
      ;; edit mode
      (list (if (or draft? can-edit?) (action-settings data))
            (if draft? (action-reset-draft))
            (if draft? (action-save))
            (if (or draft? can-edit?) (action-upload))
            (action-select-mode))
      ;; read mode
      (list (action-datasheet)
            (action-report)
            (if (and topsoe? gold-cup?) (action-publish-gold-cup))
            (if (and dirty? (or draft? can-edit?)) (action-upload))
            (if (and (not draft?) can-delete?) (action-delete))
            (action-dataset-list)
            (if (or draft? can-edit?) (action-select-mode))))))

(defn body [{:keys [width height]}]
  (let [area-opts @(rf/subscribe [::subs/area-opts])
        selected-area @(rf/subscribe [::subs/selected-area])
        level-opts @(rf/subscribe [::subs/level-opts])
        selected-level @(rf/subscribe [::subs/selected-level])
        firing @(rf/subscribe [::subs/firing])
        sel-area-id @(rf/subscribe [::subs/selected-area-id])]
    ;; dummy for performance reason to keep the subs alive
    @(rf/subscribe [::subs/selected-area-id])
    [app-view/tab-layout
     {:top-tabs {:selected (or selected-area 0)
                 :on-select #(rf/dispatch [::event/select-area %])
                 :labels (mapv :label area-opts)}
      :bottom-tabs (if-not (or (= firing "side") (= :burner sel-area-id))
                     {:selected (or selected-level 0)
                      :on-select #(rf/dispatch [::event/select-level %])
                      :labels (mapv :label level-opts)})
      :width width, :height height
      :content
      (fn [{:keys [width height selected]}]
        (let [mode @(rf/subscribe [::subs/mode])
              [sel-top sel-bottom] selected
              view-size {:width width
                         :height height}
              area-id @(rf/subscribe [::subs/area-id sel-top])]
          (cond
            (= :edit mode) ;; twt-entry, burner, gold-cup
            (case area-id
              :twt ;; twt-entry
              [twt-entry {:level sel-bottom
                          :view-size view-size}]
              :burner ;; burner-entry
              [burner-entry {:view-size view-size}]
              :gold-cup ;; gold-cup-entry
              [:div {:style view-size} "gold-cup entry"]
              nil)

            (= :read mode)
            (case area-id
              :overall ;; overall
              [overall-graph {:level sel-bottom
                              :view-size view-size}]
              :twt ;; twt-graph
              [twt-graph {:level sel-bottom
                          :view-size view-size}]
              :burner ;; burner-status
              [burner-status {:view-size view-size}]
              :gold-cup ;; gold-cup-view
              [:div {:style view-size} "gold-cup view"]
              nil))))}]))

(defn fetching [{:keys [width height]}]
  [:div {:style {:widht width, :height height
                 :border-radius "8px"
                 :border (str "1px solid " (color-hex :sky-blue))
                 :padding "20px"
                 :background (color-hex :white)}}
   [ui/circular-progress]])

(defn not-found [{:keys [width height]}]
  [:div {:style {:width width, :height height
                 :border-radius "8px"
                 :border (str "1px solid " (color-hex :sky-blue))
                 :padding "20px"
                 :font-size "16px"
                 :background (color-hex :white)
                 :color (color-hex :amber)}}
   (if (= :read @(rf/subscribe [::subs/mode]))
     [:span
      [ui/font-icon {:class-name "fa fa-exclamation-triangle"
                     :style {:color (color-hex :amber)
                             :margin-right "10px"}}]
      (translate [:dataset :message :not-found] "Not found!")])])

(defn not-ready [{:keys [width height]}]
  [:div {:style {:width width, :height height}}
   [:div (use-style style/no-config)
    (if @(rf/subscribe [::app-subs/config?])
      ;; configured but settings not ready
      (translate [:warning :no-settings :message]
                 "Plant settings not initialized!")
      ;; not configured yet!
      (translate [:warning :no-config :message]
                 "Plant configuration not initialized!"))]])

(defn dataset [{:keys [size]}]
  (let [settings? @(rf/subscribe [::subs/settings?])
        last-saved @(rf/subscribe [::subs/last-saved])
        data-date @(rf/subscribe [::subs/data-date])
        fetching? @(rf/subscribe [::subs/fetching?])]
    [:div
     [app-view/layout-main
      size
      (str (translate [:dataset :title :text] "Dataset")
           ": "
           (if data-date
             (let [{:keys [year month day hour minute]}
                   (u/to-date-time-map data-date)]
               (format "%4d-%02d-%02d | %02d:%02d"
                       year month day hour minute))))
      (str (translate [:dataset :sub-title :text] "Last saved")
           ": "
           (if last-saved
             (let [{:keys [year month day hour minute second]}
                   (u/to-date-time-map last-saved)]
               (format "%4d-%02d-%02d | %02d:%02d:%02d"
                       year month day hour minute second))))
      (if data-date
        (get-actions)
        (list (action-dataset-list)))
      (if-not settings?
        not-ready
        (if fetching?
          fetching
          (if data-date
            body
            not-found)))]

     ;;dataset settings dialog
     (if @(rf/subscribe [:tta.dialog.dataset-settings.subs/open?])
       [dataset-settings])]))
