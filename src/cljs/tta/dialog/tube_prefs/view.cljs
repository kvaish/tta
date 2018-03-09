;; view elements dialog tube-prefs
(ns tta.dialog.tube-prefs.view
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
            [ht.util.interop :as i]
            [tta.dialog.tube-prefs.style :as style]
            [tta.dialog.tube-prefs.subs :as subs]
            [tta.dialog.tube-prefs.event :as event]
            [ht.app.comp :as ht-comp]
            [tta.app.comp :as app-comp]
            [tta.app.icon :as ic]
            [tta.app.input :refer [tube-pref-list]]))

(def my-container (r/atom {}))



(defn set-tube-pref [row ind value]
  (rf/dispatch [::event/set-field row ind value]))

(defn pref [row ind]
  @(rf/subscribe [::subs/field (conj [] row ind)]))

(defn clear-tube-prefs [])

#_(defn render-tube-prefs [{:keys [tube-prefs my-container on-select pref-fn ]}])

(defn render-tube-prefs [{:keys [rows  on-select pref-fn ]}]
  [:div {:ref "container"}
   [tube-pref-list {:width (:width @my-container)
                    :height 400
                    :item-height 56
                    :item-width 250
                    :on-select on-select
                    :on-clear #(clear-tube-prefs)
                    :pref-fn pref-fn
                    :rows  rows
                    :plant @(rf/subscribe [::app-subs/plant])}]])
(defn- tube-prefs-component [tube-prefs]
  (let [i nil ind nil]
    (r/create-class
     {:component-did-mount 
      (fn [this]

        (swap! my-container  assoc
               :width (i/oget-in this [:refs :container :offsetWidth])
               :height 400))
      :component-did-update
      #(render-tube-prefs {
                           :item-width 250
                           :on-select set-tube-pref
                           :pref-fn pref
                           :rows  tube-prefs
                           :plant @(rf/subscribe [::app-subs/plant])})
      
      :reagent-render
      (fn []
        (render-tube-prefs {
                            :item-width 250
                            :on-select set-tube-pref
                            :on-clear #(clear-tube-prefs)
                            :pref-fn pref
                            :rows  tube-prefs
                            :plant @(rf/subscribe [::app-subs/plant])}))})))
(defn tube-prefs []
  (let [open? @(rf/subscribe [::subs/open?])
        tube-prefs @(rf/subscribe [::subs/data])
        title (translate [:tube-preference :dialog :title] "Tube Preferance")
        close-tooltip (translate [:app :tooltip :close] "Close")
        on-close #(rf/dispatch [::event/close])] 
    
    [ui/dialog
     {:open open?
      :modal true
      :title (r/as-element (ht-comp/optional-dialog-head
                            {:title title
                             :on-close on-close
                             :close-tooltip close-tooltip}))

      :actions (r/as-element
                [:div
                 [app-comp/button {:icon ic/cancel
                                   :label (translate [:action :cancel :label] "Cancel")
                                   :on-click #(rf/dispatch [::event/cancel])}]
                 [app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
                                   :icon ic/accept
                                   :label (translate [:action :done :label] "Done")
                                   :on-click #(rf/dispatch [::event/submit])}]])}
     [:div
      (swap! my-container  assoc
             :tube-prefs tube-prefs)
      [tube-prefs-component tube-prefs]]]))
