;; view elements dialog choose-client
(ns tta.dialog.choose-client.view
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
            [tta.dialog.choose-client.style :as style]
            [tta.dialog.choose-client.subs :as subs]
            [tta.dialog.choose-client.event :as event]))
(defonce id (atom 0))
(defn choose-client []
  (let [open? @(rf/subscribe [::subs/open?])
        countries @(rf/subscribe [::app-subs/country-list])
        value (:value @(rf/subscribe [::subs/field :country]))
        client-list @(rf/subscribe [::subs/client-list])
        selected-client @(rf/subscribe [::subs/selected-client])]
    
    [ui/dialog
     {:open  open?
      :modal true
      :title (translate [:chooseClientPrompt :title :label] "Choose Client")}
     [:div (use-style style/select-client-container)
      [:p (use-sub-style style/select-client-container :p)
       (translate [:chooseClientPrompt :searchClient :text]
                  "Specify any of the following to search for clients")]
      [:div (use-style style/select-client-filters)
       (r/as-element [ui/text-field
                      (merge  (use-style style/filter-fields)
                              {:hint-text (translate
                                           [:chooseClientPrompt
                                            :shortName :label] "Short Name")
                               :label (translate [:chooseClientPrompt
                                                  :shortName :label] "Short Name")
                               :on-change #(rf/dispatch [::event/change-field
                                                         :short-name %2
                                                         (swap! id inc)] )})])
       [ui/text-field
        (merge (use-style style/filter-fields)
               {:hint-text (translate [:chooseClientPrompt :name :label] "Name")
                :label (translate
                        [:chooseClientPrompt :name :label]
                        "Name")
                :on-change #(rf/dispatch [::event/change-field :name
                                          %2
                                          (swap! id inc)])
                :name "name"})]
       [ui/text-field
        (merge (use-style style/filter-fields)
               {:hint-text (translate
                            [:chooseClientPrompt :location :label]
                            "Location")
                :label (translate
                        [:chooseClientPrompt :location :label]
                        "Location")
                :on-change #(rf/dispatch [::event/change-field
                                          :location
                                          %2
                                          (swap! id inc)] )
                :name "location"})]

       (into [ui/select-field
              (merge (use-style style/filter-fields)
                     {:style {:vertical-align "top"}
                      :label (translate [:chooseClientPrompt :country :label]
                                        "Country")
                      :value value
                      :on-change #(rf/dispatch [::event/change-field
                                                :country
                                                %3
                                                (swap! id inc)] )
                      :name "country"})]

             (map (fn [a]
                    [ui/menu-item {:key a
                                   :value a
                                   :primary-text a}]) countries))  
       
       [ui/radio-button
        (merge (use-style style/filter-fields)
               {:style {:height "50px"}
                :label (translate [:chooseClientPrompt :havePlant :label]
                                  "Have Plant")}
               )]]
      [:div (use-style style/results-container)
       #_[:div (use-style style/filter-results-busy)]
       [:div (use-style style/results-header)
        "Choose Client"]
       [:ul (use-sub-style style/filter-results :ul)
        (if (empty? client-list)
          [:li (merge (use-sub-style style/filter-results :li))
           [:p [:i "No clients found."]]]
          (map (fn [res] [:li (merge (use-sub-style style/filter-results :li)
                                    {:key (:id res)
                                     :on-click #(rf/dispatch
                                                 [::event/select-client res])})

                         [:p [:b (:name res)]]
                         [:p [:i (get-in res  [:address :address])]]
                         [:p [:i (str
                                  (get-in res  [:address :po-box-name])
                                  "-"
                                  (get-in res  [:address :po-city])
                                  "-"
                                  (get-in res [:address :zip-code]))]]
                         [:p [:i (str (get-in res  [:short-name])
                                      "-"
                                      (get-in res [:location])
                                      "-"
                                      (get-in res  [:city])
                                      "-"
                                      (get-in res [:state])
                                      "-"
                                      (get-in res [:country]))]]
                         #_[:p [:i (:name res)]]
                         #_[:span [:b (:short-name res)]
                            #_[:span {:style {:margin-left "10px"}}
                               #_(:location res)]]]) client-list))]]
      
      (if selected-client [:div (use-style style/selected-client)
                           [:p [:b (:name selected-client)]]
                           [:p [:i (str (:country selected-client)
                                        ", " (:state selected-client)
                                        ", " (:city selected-client))]]
                           [ui/raised-button (merge
                                              (use-sub-style
                                               style/selected-client :button)
                                              {:label (translate
                                                       [:choosePlantPrompt :lavel]
                                                       "Select Client")
                                               :on-click #(rf/dispatch
                                                           [::event/set-active-client
                                                            selected-client])}
                                              )]])]]))
