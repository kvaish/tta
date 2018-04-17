;; view elements component dataset-selector
(ns tta.component.dataset-selector.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [reagent.dom :as dom]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.util.common :refer [from-date-time-map to-date-time-map]]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.app.icon :as ic]
            [ht.util.interop :as i]
            [ht.util.common :as u]
            [tta.app.comp :as app-comp]
            [tta.app.style :as app-style]
            [tta.component.dataset-selector.style :as style]
            [tta.component.dataset-selector.subs :as subs]
            [tta.component.dataset-selector.event :as event]
            [tta.component.dataset.event :as dataset-event]))

(def state (r/atom {}))

(defn popover [props & children]
  (into [ui/popover (merge props (use-style app-style/popover))]
        children))

(defn r-element [entity]
  (r/as-element [:div {:style {:margin "0px 10px"
                               :color "black"}} entity]))

(defn display-date [date]
  (let [{:keys [day month year hour minute]} (to-date-time-map date)]
    ;(str year "-" month "-" day "|" hour ":" minute)
    (subs date 0 25)))

(defn menu [selected items]
  (into [ui/menu {:value           selected
                  :menu-item-style {:font-size   "12px"
                                    :line-height "24px"
                                    :min-height  "24px"
                                    :color "#002856"}}]
        (map
          (fn [item]
            [ui/menu-item
             {:key             (:id item)
              :primary-text    (display-date (:data-date item))
              :left-icon       (if (:topsoe? item) (r-element "◆"))
              :right-icon      (if (< (:tubes% item) 70)
                                 (r-element [ic/dataset-inadequate])
                                 (if (<= 70 (:tubes% item) 85)
                                   (r-element [ic/dataset-incomplete])
                                   nil))
              :on-click        #(do
                                  (swap! state assoc :open? false)
                                  (rf/dispatch [::dataset-event/init
                                                {:dataset-id (:id item)}]))
              :disabled        false ;(disabled? item)
              :value           (:id item)
              :inner-div-style {:padding "0px 40px"}}])
          items)))

(defn dataset-selector
  [{:keys [selected date-range disabled?]}]
  (let [{:keys [height]} @(rf/subscribe [::ht-subs/view-size])
        h (* height 0.5)
        items @(rf/subscribe [::subs/data])
        date-range (rf/dispatch [::event/set-date-range
                                 (or date-range
                                     {:start (t/ago (t/period :months 1))
                                      :end (t/now)})])]
    (r/create-class
      {:component-did-mount (fn [this]
                              (swap! state assoc :anchor (dom/dom-node this)))

       :reagent-render      (fn [{:keys [selected date-range disabled?]}]
                              (let [{:keys [open? anchor]} @state]
                                [:span {:style {:display        "inline-block"
                                                :padding        "0"
                                                :vertical-align "top"}}
                                 [app-comp/button
                                  {:label    "Dataset"
                                   :disabled? disabled?
                                   :icon     ic/dataset
                                   :on-click #(swap! state assoc :open? true)}]
                                 [popover
                                  {:open             (:open? @state)
                                   :anchor-el        (:anchor @state)
                                   :anchor-origin    {:horizontal "left",
                                                      :vertical   "bottom"}
                                   :target-origin    {:horizontal "middle"
                                                      :vertical   "top"}
                                   :on-request-close #(swap! state assoc :open? false)}
                                  [menu selected items]]]))})))