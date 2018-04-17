;; view elements component dataset-filter
(ns tta.component.dataset-filter.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [reagent.dom :as dom]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.app.icon :as ic]
            [ht.util.interop :as i]
            [tta.app.comp :as app-comp]
            [tta.app.style :as app-style]
            [tta.component.dataset-filter.style :as style]
            [tta.component.dataset-filter.subs :as subs]
            [tta.component.dataset-filter.event :as event]))

(def date-formatter (tf/formatter "YYYY-MM-DD HH:mm:ssZ"))

(defn format-date [date] (tf/unparse date-formatter (t/date-time date)))

(defn popover [props & children]
  (into [ui/popover (merge props (use-style app-style/popover))]
        children))

(defn on-select [item i]
  (js/console.log "clicked" item))

(defn menu [selected items state]
  (into [ui/menu {:value           selected
                  :menu-item-style {:font-size   "12px"
                                    :line-height "24px"
                                    :min-height  "24px"}}]
        (map
          (fn [item i]
            [ui/menu-item
             {:key          i
              :primary-text (:data-date item)
              :left-icon    (if (:topsoe? item)
                              (r/as-element [:div {:style {:margin "0px 10px"}} "◆"]))
              :right-icon   (if (< (:tubes% item) 70)
                              (r/as-element [:div {:style {:margin "0px 10px"}}
                                             [ic/dataset-inadequate]])
                              (if (<= 70 (:tubes% item) 85)
                                (r/as-element [:div {:style {:margin "0px 10px"}}
                                               [ic/dataset-incomplete]])
                                nil))
              :on-click     #(do
                               (swap! state assoc :open? false)
                               (on-select item i))
              :disabled     false ;(disabled?-fn item)
              :value        (:data-date item)
              :inner-div-style {:padding "0px 40px"}}])
          items (range))))

(defn dataset-filter
  [{:keys [selected date-range]}]
  (let [{:keys [height]} @(rf/subscribe [::ht-subs/view-size])
        h (* height 0.5)
        state (r/atom {})]
    (r/create-class
      {:component-did-mount (fn [this]
                              (swap! state assoc :anchor (dom/dom-node this)))

       :reagent-render      (fn [{:keys [selected date-range]}]
                              (let [{:keys [open? anchor]} @state
                                    date-range (rf/dispatch [::event/set-date-range
                                                             (or date-range
                                                                 {:start (format-date (t/now))
                                                                  :end   (format-date (t/ago (t/period :months 1)))})])
                                    items @(rf/subscribe [::subs/data])]
                                [:span {:style {:display        "inline-block"
                                                :padding        "0"
                                                :vertical-align "top"}}
                                 [app-comp/button
                                  {:label    "Dataset"
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
                                  [menu selected items state]]]))})))