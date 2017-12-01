(ns tta.component.root.views
  (:require [re-frame.core :as rf]
            [stylefy.core :refer [use-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.subs :as app-subs]
            [tta.app.events :as app-events]
            [tta.util.gen :as u]
            [tta.component.root.style :as style]
            [tta.component.root.subs :as subs]
            [tta.component.root.events :as events]
            [tta.component.home.views :as home]
            [tta.component.dataset.views :as dataset]
            [tta.component.trendline.views :as trendline]
            [tta.component.dataentry.views :as dataentry]
            [tta.component.logger.views :as logger]))

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


(defn main-container []
  (let [view-size @(rf/subscribe [::app-subs/view-size])]
    [:div (update (use-style style/main-container) :style
                  assoc :height (style/main-container-height view-size))
     (let [active-link @(rf/subscribe [::subs/active-menu-link])]
       (js/console.log active-link "check menu link")
       [:div {:style {:height "100%"}}
       (case active-link
         :home [home/home-comp]
         :dataset  [dataset/dataset-com]
         :trendline [trendline/trendline-com]
         :dataentry [dataentry/dataentry-com]
         :logger  [logger/logger-com]
         )])



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






