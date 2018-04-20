;; view elements component dataset-selector
(ns tta.component.dataset-selector.view
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [reagent.format :refer [format]]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.util.common :refer [from-date-time-map to-date-time-map]]
            [ht.style :refer [color-hex]]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.app.icon :as ic]
            [tta.app.scroll :refer [lazy-rows]]
            [ht.util.interop :as i]
            [ht.util.common :as u]
            [tta.app.comp :as app-comp]
            [tta.app.style :as app-style]
            [tta.component.dataset-selector.style :as style]
            [tta.component.dataset-selector.subs :as subs]
            [tta.component.dataset-selector.event :as event]))

(defn left-icon [topsoe?]
  [:span {:style {:margin "0px 10px"
                 :color (color-hex :bitumen-grey)}}
   (if topsoe? "◆" "")])

(defn right-icon [tubes%]
  [:span {:style {:margin "0px 10px"}}
   (if (< tubes% 70)
     [ic/dataset-inadequate {:style {:color (color-hex :red)
                                     :position "absolute"}}]
     (if (<= 70 tubes% 85)
       [ic/dataset-incomplete {:style {:color (color-hex :amber)
                                       :position "absolute"}}]
       nil))])

(defn display-date [data-date]
  (let [{:keys [year month day hour minute]}
        (u/to-date-time-map data-date)]

    [:div {:style {:width       120
                   :font-size   "12px"
                   :line-height "24px"
                   :min-height  "24px"
                   :color       (color-hex :royal-blue)
                   :display     "inline-block"}}
     (format "%4d-%02d-%02d | %02d:%02d"
             year month day hour minute)]))

(defn dataset-list [datasets state]
  (let [{:keys [height]} @(rf/subscribe [::ht-subs/view-size])
        h (* height 0.4), w 200
        icon-style {:width   40
                    :display "inline-block"}
        style (use-style style/item-style)]
    [lazy-rows
     {:width           w
      :height          h
      :item-height     32
      :item-count      (count datasets)
      :items-render-fn #(map (fn [item]
                               [:div {:id (:id item)
                                      :style {:style {:cursor "pointer"}}
                                      :on-click (fn []
                                                  (swap! state assoc :open? false)
                                                  (rf/dispatch [::event/select-dataset (:id item)]))}
                                ;;left icon
                                [:div {:style icon-style}
                                 (left-icon (:topsoe? item))]
                                ;;date
                                [display-date (:data-date item)]
                                ;;right icon
                                [:div {:style icon-style}
                                 (right-icon (:tubes% item))]
                                ]) datasets)}]))

(defn menu [state selected-id warn-on-selection-change?]
  (let [fetching? @(rf/subscribe [::subs/fetching?])
        datasets @(rf/subscribe [::subs/data])
        datasets [{:id "a"
                   :data-date (js/Date.)
                   :topsoe? true
                   :tubes% 65}
                  {:id "b"
                   :data-date (js/Date.)
                   :topsoe? true
                   :tubes% 85}
                  {:id "c"
                   :data-date (js/Date.)
                   :topsoe? false
                   :tubes% 80}
                  {:id "d"
                   :data-date (js/Date.)
                   :topsoe? false
                   :tubes% 70}]]
    [ui/menu {:value           selected-id
              :menu-item-style {:font-size   "12px"
                                :line-height "24px"
                                :min-height  "24px"
                                :color (color-hex :royal-blue)}}
     (cond
       ;; show busy
       fetching? [ui/menu-item {}
                  [ui/circular-progress {:width "20px", :height "20px"}]]
       ;; show not found
       (empty? datasets) [ui/menu-item {}
                          (translate [:dataset-selector :message :not-found]
                                     "no datasets found")]
       ;; dataset list
       :found-datasets [dataset-list datasets state])]))

(defn dataset-selector [{:keys [selected-id warn-on-selection-change?]}]
  (let [state (r/atom {:open? false})]
    (r/create-class
     {:component-did-mount (fn [this]
                             (swap! state assoc :anchor (dom/dom-node this)))
      :reagent-render
      (fn [{:keys [selected-id warn-on-selection-change?]}]
        (let [{:keys [open? anchor]} @state]
          [:span
           [app-comp/button
            {:label (translate [:dataset-selector :anchor :label]
                               "Dataset")
             :icon ic/dataset
             :on-click #(swap! state assoc :open? true)}]
           [app-comp/popover {:open             open?
                              :anchor-el        anchor
                              :anchor-origin    {:horizontal "right",
                                                 :vertical   "bottom"}
                              :target-origin    {:horizontal "right"
                                                 :vertical   "top"}
                              :on-request-close #(swap! state assoc :open? false)}
            [menu state selected-id warn-on-selection-change?]]]))})))
