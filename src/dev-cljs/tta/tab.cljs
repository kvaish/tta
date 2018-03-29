(ns tta.tab
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [tta.app.view :refer [tab-layout]]
            [tta.app.style :as app-style]
            [ht.style :as ht-style]
            [tta.tube-list :refer [tube-list]]))

(defonce state (r/atom {:top 0
                        :bottom 0}))

(defn tab []
  [:div {:style {:border "1px solid grey"
                 :padding "20px"
                 :width 640, :height 490}}
   [tab-layout {:top-tabs {:selected (:top @state)
                           :on-select #(swap! state assoc :top %)
                           :labels ["Tube" "Burners"]}
                :bottom-tabs {:selected (:bottom @state)
                              :on-select #(swap! state assoc :bottom %)
                              :labels ["Top" "Middle" "Bottom"]}
                :width 600, :height 450
                :content
                (fn [{:keys [width height selected]}]
                  (let [lighten (if (= 0 (first selected)) 0 20)]
                    [:div {:style
                           {:width width, :height height
                            :color "white"
                            :font-size "32px"
                            :padding "20px"
                            :background
                            (get [(ht-style/color-hex :sky-blue lighten)
                                  (ht-style/color-hex :ocean-blue lighten)
                                  (ht-style/color-hex :monet-pink lighten)]
                                 (second selected))}}
                     [:div {:style {:display "inline-block"
                                    :vertical-align "top"}}
                      [:span "content"]
                      [:p "Top tab: " (first selected)]
                      [:p "Bottom tab: " (second selected)]]
                     [:div {:style {:display "inline-block"
                                    :vertical-align "top"
                                    :margin-left 50
                                    :background app-style/widget-fg}}
                      ]]))}]])
