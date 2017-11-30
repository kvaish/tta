(ns tta.component.root.views
  (:require [re-frame.core :as rf]
            [stylefy.core :refer [use-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.subs :as app-subs]
            [tta.app.events :as app-events]
            [tta.util.gen :as u]
            [tta.component.root.style :as style]
            [tta.component.root.subs :as subs]
            [tta.component.root.events :as events]))

;;;;;;;;;; top-bar start
(defn top-bar-link [props]
  (let [{:keys [icon label action]} props]
    [:a (merge (use-style style/top-bar-menu-link)
               {:href "#" :on-click action })
     (if icon [:i {:class-name icon :style {:padding "5px"}}])
     label]))

(defn top-bar []
  [:div (use-style style/top-bar)
   [:div (use-style style/top-bar-logo)]
   [:div (use-style style/pull-right)
    [top-bar-link {:icon "fa fa-caret-right"
                   :label "Settings"
                   :action (fn []
                             (js/console.log "Settings"))}]
    [top-bar-link {:label "Logout"
                   :action (fn []
                             (js/console.log "Logout"))}]
    ]])
;;;;;;;;;;;;;;;;;;; top-bar end


;;;;;;;;;;; menur-bar start
(defn menu-bar-icon []
  [:div (merge (use-style style/pull-left) {:style {:padding "12px 24px"
                                                    :width "9%"}})
   [:span {:style {:font-familty "arial"
                   :font-weight "900"
                   :font-size "18px"}} "True"]
   [:span {:style {:font-weight "300"
                   :font-size "18px"}} "Temp™"]])


(defn menu-bar-link []
  (let [active-link @(rf/subscribe [::subs/active-menu-link])]
    [:div
     (merge (use-style style/pull-left)
            {:style {:width "60%"
                     :padding "15px 10px 0 0"
                     :font-size "12px"}})
     (doall
      (map (fn [[title id] i]
             ^{:key i}
             [:a
              (merge (use-style (merge style/menu-bar-spacing
                                       (if (= active-link id)
                                         style/menu-bar-active)))
                     {:href "#"
                      :on-click #(rf/dispatch
                                  [::events/set-active-menu-link id])})
              title])
           [["Home" :home]
            ["Dataset" :dataset]
            ["Trendline" :trendline]]
           (range)))]))


(defn menu-bar-info []
  [:div {:style {:background-color "#e8e8e8"
                 :height "50px"
                 :float "right"
                 :width "25%"}}
   [:div
    (merge (use-style style/pull-left)
           {:style {:padding "2% 42% 1% 25px"}})
    [:span {:style {:display "block"
                    :font-size "10px":font-weight "300"}}
     "Company"]
    [:span {:style {:display "block"
                    :font-size "14px"}} "Vestas"]]
   [:div (merge (use-style style/pull-left)
                {:style {:padding "2% 2% 1% 2%"}})
    [:span {:style {:display "block"
                    :font-size "10px" :font-weight "300"}} "Plant"]
    [:span  {:style {:display "block"
                     :font-size "14px"}}
     "Ringsted"]]])

(defn menu-bar []
  [:div (use-style style/menu-bar)
   [menu-bar-icon]
   [menu-bar-link]
   [menu-bar-info]
   ])

;;;;;;;;; menu-bar-end

;;;;;;;;;;; main-container start
 (defn primary-row-comp "pass type anchor/paragraph" [key-v]
   (let [{:keys [type heading first-data second-data third-data icon ]} key-v]
     [ui/paper {:style {:width "32%" :height "100%" :background-color "#fff" :margin "1% 0 1% 1%" :float "left" :opacity "0.8" }}
       [:div {:style {:padding"2%" :margin "2% 1% 0 1%"}}
        [:span {:style {:font-size "30px" :font-weight "800" :color "#002856" :padding-left "5%"}} heading [:hr {:style {:position "absolute"
                                                                                                                         :margin  "0 1.5%"
                                                                                                                         :width  "35px"
                                                                                                                         :border "3px solid"
                                                                                                                         :border-width "thin"}}]]
        (if  (= type "anchor" )
        [:ul {:style {:list-style-type "none" :color "#002856" :font-size "30px" :font-weight "150" :padding-left "2%"}}
         [:li [ui/flat-button {:label first-data :label-style {:font-size "25px"
                                                         :font-weight "300"
                                                         :color "#002856" } } ]]
         [:li [ui/flat-button {:label second-data :label-style {:font-size "25px "
                                                                :font-weight "300"
                                                                :color "#002856"} } ]]
         [:li [ui/flat-button {:label third-data :label-style {:font-size "25px "
                                                               :font-weight "300"
                                                               :color "#002856"} } ]]]
        )
        (if (= type "paragraph")
          [:p {:style { :color "#002856"
                       :font-size "25px"
                       :font-weight "300"
                       :padding-left "5%"}} first-data]
          )
        [:i (merge  {:class-name "fa fa-home fa-5x"
                     :style {:padding "8% 2%" :float "right"} } ) icon]
        ]]))

(defn main-container-primery-row []
  [:div {:style {:height "60%"}}
   [primary-row-comp {:type "anchor" :heading "Create Dataset" :first-data "Data Entery" :second-data "Import From Logger App" :third-data "Print Logsheet pdf" }]
   [primary-row-comp {:type "paragraph" :heading "Analyses Dataset" :first-data "Preview the Overall,TWT,Burner status of latest publish dataset"  }]
   [primary-row-comp {:type "paragraph" :heading "Trendline Graph" :first-data "Overview of all the recorded datasets"  }]
   ])

(defn secondary-row-comp "pass type anchor/paragraph" [key-v]
  (let [{:keys [ heading data ]} key-v]
    [ui/paper  {:style {:width "18.8%" :height "100%"   :margin "0 0 1% 1%" :float "left" :opacity "0.8" }}
     [:div {:style {:padding "8%" :color "#002856"  }}
      [:span {:style {:font-weight "600" :font-size "20px" :color "#002856" :width "100%" } } heading [:hr {:style {:position "absolute"
                                                                                                     :margin  "0"
                                                                                                     :width  "20px"
                                                                                                     :border "1px solid" }} ]]
      (if data
        [:div {:style { :position "relative" }}
         [:p {:style {:display "block" :word-break "" :width "100%" :position "absolute"  }} data]
         ])]]))

(defn main-container-secondery-row []
  [:div {:style {:display "inline-block" :width :100% :height "33%"}}
   [secondary-row-comp {:heading "Gold Cup" :data "Internal users only"}]
   [secondary-row-comp {:heading "Plant Settings" :data "Manage Pyrometer custom emmissivity and role type"}]
   [secondary-row-comp {:heading "Configure Plant" :data "Internal users only"}]
   [secondary-row-comp {:heading "Reformer History" }]
   [secondary-row-comp {:heading "Logs" :data "All deleted dataset logs that can be auto recovered"}]
   ;[:div {:style {:width "18.8%" :height "100%" :background-color "#fff"  :margin "1% 0 1% 1%" :float "left" :opacity "0.8" }} [gold-cup] ]
   ;[:div {:style {:width "18.8%" :height "100%" :background-color "#fff"  :margin "1% 0 1% 1%" :float "left" :opacity "0.8" }} [plant-setting]]
   ;[:div {:style {:width "18.8%" :height "100%" :background-color "#fff"  :margin "1% 0 1% 1%" :float "left" :opacity "0.8" }} [configure-plant] ]
   ;[:div {:style {:width "18.8%" :height "100%" :background-color "#fff"  :margin "1% 0 1% 1%" :float "left" :opacity "0.8" }} [reformer-history]]
   ;[:div {:style {:width "18.8%" :height "100%" :background-color "#fff"  :margin "1% 0 1% 1%" :float "left" :opacity "0.8" }} [logs]]
   ])

(defn main-container []
  (let [view-size @(rf/subscribe [::app-subs/view-size])]
    [:div (update (use-style style/main-container) :style
                  assoc :height (style/main-container-height view-size))

     [main-container-primery-row]
     [main-container-secondery-row]


     ;for translation
     ;(u/translate [:main :greet :label] "Hi")
     ;[:br]
     ;[ui/flat-button {:label "Set language"
     ;                 :secondary true
     ;                 :on-click #(rf/dispatch [::app-events/set-language :ru])}]

     ]))
;;;;;;;;;;; main-container end

(defn root []
  [:div (use-style style/root)
   [top-bar]
   [menu-bar]
   [main-container]])



;(defn create-data-set []
;  [:div {:style {:padding"2%"}}
;   [:span {:style {:font-size "30px" :font-weight "800" :color "#002856" :padding-left "5%"}} "Create Dataset"]
;   [:ul {:style {:list-style-type "none" :color "#002856" :font-size "30px" :font-weight "150" :padding-left "5%"}}
;    [:li "Data Entry"]
;    [:li "Import From Logger App"]
;    [:li "Print Logsheet pdf"]]
;   [:i (merge  {:class-name "fa fa-home fa-5x"
;                                            :style {:padding "8% 2%" :float "right"} })]
;   ])
;
;(defn analyse-dataset []
;  [:div {:style {:padding "2%"}}
;   [:span {:style {:font-size "30px" :font-weight "800" :color "#002856" :padding-left "5%"}} "Analyse Dataset"]
;   [:ul {:style {:list-style-type "none" :color "#002856" :font-size "30px" :font-weight "150" :padding-left "5%"}}
;    [:li "Preview the Overall,"]
;    [:li "TWT,Burner status of latest"]
;    [:li "publish dataset"]]
;   [:i (merge {:class-name "fa fa-home fa-5x"
;                                            :style {:padding "8% 2%" :float "right"} })]])
;
;(defn trendline-graph []
;  [:div {:style {:padding "2%"}}
;   [:span {:style {:font-size "30px" :font-weight "800" :color "#002856" :padding-left "5%"}} "Trendline Graph"]
;   [:ul {:style {:list-style-type "none" :color "#002856" :font-size "30px" :font-weight "150" :padding-left "5%"}}
;    [:li "Overview of all the"]
;    [:li "recorded datasets"]
;    ]
;   [:i (merge {:class-name "fa fa-home fa-5x"
;                                            :style {:padding "10% 2%" :float "right"} }) ]])
;(defn main-container-primery-row []
;  [:div {:style {:height "60%"}}
;   [:div {:style {:width "32%" :height "100%" :background-color "#fff"  :margin "2% 1% 0 1%" :float "left" :opacity "0.8" }} [create-data-set ]]
;   [:div {:style {:width "32%" :height "100%" :background-color "#fff" :margin "2% 0 0 0" :float "left" :opacity "0.8" }}  [analyse-dataset]]
;   [:div {:style {:width "32%" :height "100%" :background-color "#fff" :margin "2% 1% 0 1%" :float "left" :opacity "0.8" }} [trendline-graph]]]
;  )

;(defn gold-cup []
;  [:div {:style {:padding "8%" :color "#002856"}}
;   [:span {:style {:font-weight "600" :font-size "20px" :color "#002856"} } "Gold Cup"]
;   [:div {:style {:position "absolute" :bottom  "8%"}}
;    [:span {:style {:display "block"}} "Internal"]
;    [:span "users only"]
;    ]
;   ]
;  )
;
;(defn plant-setting []
;  [:div {:style {:padding "8%" :color "#002856"}}
;   [:span  {:style {:font-weight "600" :font-size "20px" :color "#002856"} } "Plant Settings"]
;   [:div {:style {:position "absolute" :bottom  "8%"}}
;    [:span {:style {:display "block"}} "Manage Pyrometer, custom"]
;    [:span "emmissivity and role type"]
;    ]
;   ]
;  )
;
;(defn configure-plant []
;  [:div  {:style {:padding "8%" :color "#002856"}}
;   [:span  {:style {:font-weight "600" :font-size "20px" :color "#002856"} } "Configure Plant"]
;   [:div {:style {:position "absolute" :bottom  "8%"}}
;    [:span {:style {:display "block"}} "Internal"]
;    [:span "users only"]
;    ]
;   ]
;  )
;
;(defn reformer-history []
;  [:div {:style {:padding "8%" :color "#002856"}}
;   [:span   {:style {:font-weight "600" :font-size "20px" :color "#002856"} } "Reformer History"]
;
;   ]
;  )
;(defn logs []
;  [:div {:style {:padding "8%" :color "#002856"}}
;   [:span  {:style {:font-weight "600" :font-size "20px" :color "#002856"} } "Logs"]
;   [:div {:style {:position "absolute" :bottom  "8%"}}
;    [:span {:style {:display "block"}} "All deleted dataset logs"]
;    [:span "that can be auto recovered"]
;    ]
;   ]
;  )



