;; view elements dialog choose-plant
(ns tta.dialog.choose-plant.view
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
            [tta.dialog.choose-plant.style :as style]
            [tta.dialog.choose-plant.subs :as subs]
            [tta.dialog.choose-plant.event :as event]))

(defn plant-comp [p]
  [ui/list-item
   {:primary-text (:name p) :style {}
    :secondary-text (:capacity p)
    :on-click #(rf/dispatch [::event/select-plant p])
    :disabled (not (:config? p))
    :right-icon-button
    (if-not (:config? p)
      (r/as-element
       [ui/icon-button
        {:tooltip (translate [:choose-plant :configure :hint]
                             "Click to configure this plant..")
         :tooltip-position "top-left"
         :icon-class-name "fa fa-wrench"
         :icon-style style/btn-config
         :on-click #(rf/dispatch [::event/configure-plant p])}]))}])

(defn choose-plant []
  [ui/dialog
   {:modal (nil? @(rf/subscribe [::app-subs/plant]))
    :open @(rf/subscribe [::subs/open?])
    :on-request-close #(rf/dispatch [::event/close])
    :title (translate [:choose-plant :title :text] "Choose plant")}

   ;; left pane - client info
   [:div (use-style style/container)
    (let [client @(rf/subscribe [::subs/client])]
      [:div (use-sub-style style/container :left)
       [:p [:b (:name client)]]
       [:p [:i (:short-name client)]
        [:br] (:location client)]
       (let [a (:address client)]
         [:p (str (:po-box-name a) " - " (:po-box a))
          [:br] (str (:po-zip-code a)
               ", " (:po-city a)
               ", " (:zip-code a))])
       [:p (:city client)
        [:br] (:state client)
        [:br] (:country client)]
       (if @(rf/subscribe [::ht-subs/topsoe?])
         [ui/flat-button
          {:label (translate [:choose-plant :change-client :label] "Change")
           :secondary true
           :on-click #(rf/dispatch [::event/change-client])}])])

    ;; right pane - plant selection
    [:div (use-sub-style style/container :right)
     (let [plants @(rf/subscribe [::subs/plants])
           list-style (use-style (style/plant-selector
                                  @(rf/subscribe [::ht-subs/view-size])))]
       (-> [ui/list list-style
            (if-not plants
              [ui/linear-progress (use-style style/progress-bar)]
              (if (empty? plants)
                [ui/list-item
                 {:disabled true
                  :secondary-text (translate [:choose-plant :no-plants :text]
                                             "No plants found!")}]))]
           (into (map plant-comp plants))))]]])
