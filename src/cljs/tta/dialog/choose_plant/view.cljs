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

(defn choose-plant []
  (let [open? @(rf/subscribe [::subs/open?])
        selected-client @(rf/subscribe
                          [:tta.dialog.choose-client.subs/selected-client])
        plant-list @(rf/subscribe [::subs/plant-list])
        fetched  @(rf/subscribe [::subs/fetched])]
    [ui/dialog
     {:open open?
      :modal true
      :title (translate [:choosePlantPrompt :lavel] "Choose Plant")}
     [:div (use-style style/container)
      [:div (use-style style/selected-client)
       [:p [:b (:name selected-client)]]
       [:p [:i (str (:country selected-client)
                    ", " (:state selected-client)
                    ", " (:city selected-client))]]
       [ui/raised-button (merge
                          (use-sub-style
                           app-style/activated-result :button)
                          {:label (translate
                                   [:choosePlantPrompt :lavel]
                                   "Change Client")
                           :on-click #(rf/dispatch
                                       [::event/change-client])})]]
      [:div (use-style app-style/result-header)
       (translate
        [:choosePlantPrompt :label]
        "Choose plant")]
      [:div
       [:ul (use-sub-style app-style/filter-results :ul)
        (if-not fetched
          [:li "Loading..."])
        (if (and (empty? plant-list) fetched)
          [:li (merge (use-sub-style app-style/filter-results :li))
           [:p [:i "No plants found."]]]
          (map (fn [res]
                 [:li (merge (use-sub-style app-style/filter-results :li)
                             {:key (:id res)
                              :on-click #(rf/dispatch
                                          [::event/select-plant
                                           (:id res)
                                           (:id selected-client)])})
                  [:p [:i (:name res)]]
                  [:p [:b (str (:capacity res)
                               "-"
                               (:capacity-unit res)
                               "-"
                               (:service res)
                               "-"
                               (:licensor res))]]
                  ]) plant-list))]]]]))
