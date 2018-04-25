(ns  tta.component.dataset.twt-graph
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.view :as app-view]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.charts :refer [twt-chart overall-twt-chart]]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]))

(defn form-cell [style error label widget]
  [:div (use-sub-style style :form-cell)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error)
    (if (fn? error) (error) error)]])

(defn form-cell-2 [style error label widget]
  [:div (use-sub-style style :form-cell-2)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error)
    (if (fn? error) (error) error)]])

(defn form-cell-200 [style error label widget]
  [:div (use-sub-style style :form-cell-200px)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error)
    (if (fn? error) (error) error)]])

(defn twt-type [style]) 

(defn reduced-firing [style]
  [form-cell-200 style nil
   (translate [:dataset :reduced-firing :label] "Reduced Firing")
   [app-comp/toggle
    {:value  @(rf/subscribe [::subs/reduced-firing-filter])
     :on-toggle #(rf/dispatch [::event/set-reduced-firing-filter
                               %])}]])

(defn avg-temp-band [style]
  [form-cell-200 style nil
   (translate [:dataset :avg-temp-band :label] "+/- 20° off Avg")
   [app-comp/toggle
    {:value  @(rf/subscribe [::subs/avg-temp-band-filter])
     :on-toggle #(rf/dispatch [::event/set-avg-temp-band-filter
                               %])}]])

(defn avg-raw-temp [style]
  (let [val @(rf/subscribe [::subs/avg-raw-temp-filter])]
      [form-cell-200 style nil
       (translate [:dataset :avg-raw-temp :label] "Avg. Raw Temp")
       [app-comp/toggle
        {:value     false
         :on-toggle #(rf/dispatch [::event/set-avg-raw-temp-filter
                                   %])}]]))

(defn avg-corrected-temp [style]
  [form-cell-200 style nil
   (translate [:dataset :avg-corrected-temp :label] "Avg. Corr. Temp")
   [app-comp/toggle
    {:value @(rf/subscribe [::subs/avg-corrected-temp-filter])
     :on-toggle #(rf/dispatch [::event/set-avg-corrected-temp-filter                               %])}]])


(defn sf-twt [{:keys [width height]}]
  (let [chambers (get-in @(rf/subscribe [::subs/config]) [:sf-config :chambers])
        item-width 800
        render-fn
        (fn [indexes show-item] 
          (map (fn [i]
                 [:div {:style {:height height
                                :width item-width}}]) 
               indexes))]
    [lazy-cols {:width width
                :height height  
                :item-width item-width
                :items-render-fn render-fn
                :item-count (count chambers)}]))


(defn tf-twt [{:keys [width height]}]
  (let [{:keys [tube-rows tube-row-count]}
        (:tf-config @(rf/subscribe [::subs/config]))
        item-width 600
        render-fn
        (fn [indexes show-item] 
          (map (fn [i]
                 (let [chart-row @(rf/subscribe [::subs/tf-twt-chart-row :top i])
                       chart-row (assoc chart-row  :height height :width item-width)]
                   [twt-chart chart-row])
                 )
               indexes))]
    [lazy-cols {:width width
                :height height  
                :item-width item-width
                :items-render-fn render-fn
                :item-count tube-row-count}]))

(defn twt-graph [{:keys [level], {:keys [width height]} :view-size}]
  (let [firing          @(rf/subscribe [::subs/firing])
        twt-type        @(rf/subscribe [::subs/twt-temp])
        h1              55
        h3              65
        h2              (- height (+ h1  h3))
        style (style/body width height)]
    [:div
     [:div {:style {:height h1
                    :font-size "14px"
                    :width width}}
      (let [twt-opts   @(rf/subscribe [::subs/twt-temp-opts])
            twt-type        @(rf/subscribe [::subs/twt-temp])
            selected (some #(if(= (:id %) twt-type) %) twt-opts)] 

        [form-cell-2 style nil 
         (translate [:dataset :reduced-firing :label] "Choose type:")
         [app-comp/selector {:item-width 70
                             :options twt-opts
                             :label-fn :label
                             :selected selected
                             :value-fn :id
                             :on-select #(rf/dispatch [::event/set-twt-temp
                                                       (% :id)])}]])]
     
     [:div {:style {:height h2 :width width }}
      (case firing
        "side" [sf-twt  {:width width
                         :height h2}]
        "top" [tf-twt  {:width width
                        :height h2}])]
     [:div {:style {:height h3 :width width}}
      (reduced-firing style)
      (avg-temp-band style)
      (avg-raw-temp style)
      (avg-corrected-temp style)
      [:div {:style {:height 30 :width width
                     :text-align "right" }} ;;TODO: chart info
        ]]]))
