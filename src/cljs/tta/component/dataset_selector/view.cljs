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
  [:span {:style {:margin "2px 10px"
                  :display "inline-block"
                  :vertical-align "top"
                  :color (color-hex :bitumen-grey)}}
   (if topsoe? "◆" "")])

(defn right-icon [tubes%]
  [:span {:style {:margin "0px 10px"
                  :display "inline-block"
                  :vertical-align "top"}}
   (if (< tubes% 70)
     [ic/dataset-inadequate {:style {:color (color-hex :red)
                                     :position "absolute"}}]
     (if (<= 70 tubes% 85)
       [ic/dataset-incomplete {:style {:color (color-hex :amber)
                                       :position "absolute"}}]
       nil))])

(defn display-date [style data-date]
  (let [{:keys [year month day hour minute]}
        (u/to-date-time-map data-date)]
    [:div (use-sub-style style :display-date)
     (format "%4d-%02d-%02d | %02d:%02d"
             year month day hour minute)]))

(defn dataset-list-item [state style index selected-id warn?]
  (let [items @(rf/subscribe [::subs/data])
        {:keys [id data-date topsoe?] :as item} (get items index)
        tubes% (get-in item [:summary :tubes%])
        selected? (= id selected-id)]
    [:div (if selected?
            (use-sub-style style :selected-item)
            (use-sub-style style :item))
     [:div
      {:id id
       :on-click (fn []
                   (swap! state assoc :open? false)
                   (rf/dispatch [::event/select-dataset id warn?]))}
      ;;left icon
      [:div {:style {:width 40, :display "inline-block"}}
       (left-icon topsoe?)]
      ;;date
      [display-date style data-date]
      ;;right icon
      [:div {:style {:width 40, :display "inline-block"}}
       (right-icon tubes%)]]]))

(defn dataset-list [state style selected-id warn?]
  (let [{:keys [height]} @(rf/subscribe [::ht-subs/view-size])
        h (* height 0.3)
        {:keys [w]} (:data (meta style))
        items @(rf/subscribe [::subs/data])]
    [lazy-rows
     {:width w, :height h
      :item-height 32
      :item-count (count items)
      :items-render-fn
      (fn [indexes _]
        (map (fn [index]
               [dataset-list-item state style index selected-id warn?])
             indexes))}]))

(defn menu [state selected-id warn?]
  (let [fetching? @(rf/subscribe [::subs/fetching?])
        style (style/dataset-list-style)
        items @(rf/subscribe [::subs/data])
        {:keys [w iw]} (:data (meta style))]
    [ui/menu {:width w, :auto-width false
              :menu-item-style {:width iw, :height 20
                                :font-size   "12px"
                                :line-height "24px"
                                :min-height  "24px"
                                :color (color-hex :royal-blue)}}
     (cond
       ;; show busy
       fetching?
       [ui/menu-item {:disabled true}
        [ui/linear-progress {:style {:margin-top "20px"}}]]
       ;; show not found
       (empty? items)
       [ui/menu-item {:disabled true}
        (translate [:dataset-selector :message :not-found] "No datasets found!")]
       ;; dataset list
       :found-datasets
       [dataset-list state style selected-id warn?])]))

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
