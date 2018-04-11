;; view elements component dataset
(ns tta.component.dataset.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [tta.app.icon :as ic]
            [ht.util.interop :as i]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [ht.util.common :as htu]
            [tta.app.view :as app-view]
            [tta.app.comp :as app-comp]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]
            [tta.dialog.dataset-settings.view :refer [dataset-settings]]
            [tta.dialog.dataset-settings.event :as dataset-setting-evt]
            [tta.component.dataset.twt-entry.view :refer [twt-entry]]
            [tta.component.dataset.twt-graph.view :refer [twt-graph]]
            [tta.component.dataset.burner-entry.view :refer [burner-entry]]
            [tta.component.dataset.overall-graph.view :refer [overall-graph]]
            [tta.component.dataset.burner-status.view
             :refer [burner-status]]))

(def date-formatter (tf/formatter "yyyy-MM-dd"))
(defn format-date [date] (tf/unparse date-formatter (t/date-time date)))

(defn show-error? []
  @(rf/subscribe [::subs/show-error?]))

(defn settings-icon [props]
  [ui/font-icon (update props :class-name str " fa fa-cog")])

;;visible only when creating draft
(defn action-save []   
  [app-comp/button {:disabled?
                    (not @(rf/subscribe [::subs/can-submit?]))
                    :icon ic/save
                    :on-click #(rf/dispatch [::event/save-draft])
                    :label (translate [:dataset :action :save]
                                      "Save")}])
;;dataset settings visible only in read mode
(defn action-setting [data]  
  [app-comp/button {:disabled?
                    (not @(rf/subscribe [::subs/can-upload?]))
                    :icon settings-icon
                    :label (translate [:dataset :action :settings]
                                      "Settings")
                    :on-click
                    #(rf/dispatch
                      [:tta.dialog.dataset-settings.event/open
                       {:dataset data}])}])

;;report download 
(defn action-report []
  [app-comp/button {:disabled? false
                    :icon ic/report
                    :label (translate
                            [:dataset :action :report]
                            "Report")
                    :on-click
                    #(rf/dispatch [::event/report])}])
;;excel download 
(defn action-excel []
  [app-comp/button {:disabled? false
                    :icon ic/report
                    :label (translate [:dataset :action :excel]
                                      "Export")
                    :on-click #(rf/dispatch [::event/excel])}])

;;upload dataset if dirty and valid or dirty
(defn action-upload []
  [app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-upload?]))
                    :icon ic/upload
                    :on-click #(rf/dispatch [::event/upload])
                    :label (translate [:dataset :action :upload]
                                      "Upload")}])

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
(defn get-actions []
  (let [data @(rf/subscribe [::subs/data])
        draft? (:draft? data)
        mode @(rf/subscribe [::subs/mode])
        actions
        (list (action-upload)
              (action-selector))]
    (if (= mode :edit) 
      (conj actions
            (if draft? (action-save))
            (action-setting data))
      (conj actions action-report action-excel))))

(defn body [{:keys [width height]}]
  (r/create-class
   {:component-did-mount
    (fn [this])
    :reagent-render
    (fn []
      (let [area-opts @(rf/subscribe [::subs/area-opts])
            selected-area @(rf/subscribe [::subs/selected-area])
            level-opts @(rf/subscribe [::subs/level-opts])
            selected-level @(rf/subscribe [::subs/selected-level])
            firing @(rf/subscribe [::subs/firing])]

        [:div {:ref "container"}
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
                   top (first selected)
                   bottom (second selected)
                   view-area {:width width
                              :height height}]
               (cond
                 (and (= top 0) (= "top" firing))
                 (if (= :edit mode) [twt-entry {:level bottom
                                                :view-size view-area}]
                     [overall-graph])

                 (and (= top 0) (= "side" firing))
                 (if (= :edit mode) [twt-entry {:level bottom
                                                :view-size view-area}]
                     [twt-graph])
                 
                 (and (= top 1) (= "top" firing))
                 (if (= :edit mode) [burner-entry] [twt-graph])

                 (and (= top 1) (= "side" firing))
                 (if (= :edit mode) [burner-entry] [burner-status]))))}]]))}))
;; dataset: date | time
;; last saved: date | time (hide when nil)

;; buttons array:
;; mode :read - upload (disable if not dirty), excel report, pdf report
;;              publish goldcup (if goldcup? and internal? us
;;(disable if not 100% or not uploaded)
;; mode :edit - settings, save, upload
;; mode selector : disabled when reformer version not current

(defn dataset [props]
  (let [last-saved @(rf/subscribe [::subs/last-saved])
        data-date @(rf/subscribe [::subs/data-date])]
    [:div
     [app-view/layout-main
      (str (translate [:dataset :title :text] "Dataset")
           ": "
           (format-date (js/Date. data-date))  " | "
           (:hour (htu/to-date-time-map last-saved)) ":"
           (:minute (htu/to-date-time-map last-saved)))
      (if last-saved
        (str (translate [:dataset :title :text] "Last saved date")
             ": "
             (format-date (js/Date. last-saved))
             " | "
             (:hour (htu/to-date-time-map last-saved))
             ":"
             (:minute (htu/to-date-time-map last-saved)))
        (str (translate [:dataset :title :text] "Last saved date")
             ": "))

      (get-actions)
      body]

     ;;dataset settings dialog
     (if @(rf/subscribe [:tta.dialog.dataset-settings.subs/open?])
       [dataset-settings])]))
