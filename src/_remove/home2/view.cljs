(ns tta.component.home.view
  (:require [re-frame.core :as rf]
            [stylefy.core :refer [use-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.subs :as app-subs]
            [tta.app.events :as app-events]
            [tta.util.gen :as u]
            [tta.component.home.style :as style]
            [tta.component.home.subs :as subs]
            [tta.component.home.events :as events]))

(defn create-dataset-link []
  [:ul {:style {:list-style-type "none" :color "#002856" :font-size "30px" :font-weight "150" :padding-left "2%"}}
   (doall
     (map (fn [[key-v] i]
            (let [{:keys [label-data action-data]} key-v]
              ^{:key i}
              [:li [ui/flat-button {:label label-data :label-style {:font-size "25px"
                                                                    :font-weight "300"
                                                                    :color "#002856" }
                                    :on-click #(rf/dispatch
                                                 [::events/set-active-menu-link action-data]) } ]]
              ))
          [[{:label-data "Data Entry" :action-data :dataentry}]
           [{:label-data "Import From Logger App" :action-data :logger}]
           [{:label-data "Print Logsheet pdf"}]] (range)))])

(defn primary-row-comp "pass type anchor/paragraph" []
  [:div {:style {:height "60%"}}
   (doall
     (map (fn [[key-v] i]
            (let [{:keys [type heading first-data second-data third-data icon ]} key-v]
              ^{:key i}
              [ui/paper {:style {:width "32%" :height "100%" :background-color "#fff" :margin "1% 0 1% 1%" :float "left" :opacity "0.8" }}
               [:div {:style {:padding"2%" :margin "2% 1% 0 1%"}}
                [:span {:style {:font-size "30px" :font-weight "800" :color "#002856" :padding-left "5%"}} heading
                 [:hr {:style {:position "absolute"
                               :width  "35px"
                               :border "3px solid"
                               :margin  "0 1.5%"
                               :border-width "thin"}}]]
                (if  (= type "anchor" )
                  ;[:ul {:style {:list-style-type "none" :color "#002856" :font-size "30px" :font-weight "150" :padding-left "2%"}}
                  ; [:li [ui/flat-button {:label first-data :label-style {:font-size "25px"
                  ;                                                       :font-weight "300"
                  ;                                                       :color "#002856" }
                  ;                       :on-click #(rf/dispatch
                  ;                                    [::events/set-active-menu-link :dataentry]) } ]]
                  ; [:li [ui/flat-button {:label second-data :label-style {:font-size "25px "
                  ;                                                        :font-weight "300"
                  ;                                                        :color "#002856"}
                  ;                       :on-click #(rf/dispatch
                  ;                                    [::events/set-active-menu-link :logger]) } ]]
                  ; [:li [ui/flat-button {:label third-data :label-style {:font-size "25px "
                  ;                                                       :font-weight "300"
                  ;                                                       :color "#002856"} } ]]]
                  [create-dataset-link ]
                  )
                (if (= type "paragraph")
                  [:p {:style { :color "#002856"
                               :font-size "25px"
                               :font-weight "300"
                               :padding-left "5%"}} first-data]
                  )
                [:i (merge  {:class-name "fa fa-home fa-5x"
                             :style {:padding "8% 2%" :float "right"} } ) icon]
                ]])

            )
          [[{:type "anchor" :heading "Create Dataset" }]
           [{:type "paragraph" :heading "Analyses Dataset" :first-data "Preview the Overall,TWT,Burner status of latest publish dataset"  }]
           [{:type "paragraph" :heading "Trendline Graph" :first-data "Overview of all the recorded datasets"  }]

           ]
          (range)))])



(defn secondary-row-comp "pass type anchor/paragraph" []
  [:div {:style {:display "inline-block" :width :100% :height "33%"}}
   (doall
     (map (fn [[key-v] i]
            (let [{:keys [ heading data ]} key-v]
              ^{:key i}
              [ui/paper  {:style {:width "18.8%" :height "100%"   :margin "0 0 1% 1%" :float "left" :opacity "0.8" }}
               [:div {:style {:padding "8%" :color "#002856"  }}
                [:span {:style {:font-weight "600" :font-size "20px" :color "#002856" :width "100%" } } heading
                 [:hr {:style {:position "absolute"
                               :margin  "0"
                               :width  "20px"
                               :border "1px solid" }} ]]
                (if data
                  [:div {:style { :position "relative" }}
                   [:p {:style {:display "block" :word-break "" :width "100%" :position "absolute"  }} data]
                   ])]]
              ))
          [[{:heading "Gold Cup" :data "Internal users only"}]
           [{:heading "Plant Settings" :data "Manage Pyrometer custom emmissivity and role type"}]
           [{:heading "Configure Plant" :data "Internal users only"}]
           [{:heading "Reformer History" }]
           [{:heading "Logs" :data "All deleted dataset logs that can be auto recovered"}]
           ]
          (range)))])

(defn home-comp []
  [:div {:style {:height "100%"}}
   [primary-row-comp]
   [secondary-row-comp]
   ])
