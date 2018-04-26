(ns  tta.component.dataset.twt-graph
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [reagent.format :refer [format]]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.view :as app-view]
            [ht.util.common :as u]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.charts :refer [twt-chart]]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]))


(defn twt-type []) 
(defn reduced-firing []
  [:span (use-style style/form-field)
   [:span (use-sub-style style/form-field :label)
    (translate [:dataset :reduced-firing :label] "Reduced Firing")]
   [app-comp/toggle
    {:value  @(rf/subscribe [::subs/reduced-firing-filter])
     :on-toggle #(rf/dispatch [::event/set-reduced-firing-filter
                               %])}]])

(defn avg-temp-band []
  (let [avg-temp-band @(rf/subscribe [::subs/avg-temp-band])]
    [:span (use-style style/form-field)
     [:span (use-sub-style style/form-field :label)
      (str "±" avg-temp-band "º"(translate [:dataset :avg-temp-band :label] "off Avg"))]
     [app-comp/toggle
      {:value  @(rf/subscribe [::subs/avg-temp-band-filter])
       :on-toggle #(rf/dispatch [::event/set-avg-temp-band-filter
                                 %])}]]))

(defn avg-raw-temp []
  (let [val @(rf/subscribe [::subs/avg-raw-temp-filter])]
    [:span (use-style style/form-field)
     [:span (use-sub-style style/form-field :label)
      (translate [:dataset :avg-raw-temp :label] "Avg. Raw Temp")]
     [app-comp/toggle
      {:value    val
       :on-toggle #(rf/dispatch [::event/set-avg-raw-temp-filter
                                 %])}]]))

(defn avg-corrected-temp []
  [:span (use-style style/form-field)
   [:span (use-sub-style style/form-field :label)
    (translate [:dataset :avg-corrected-temp :label] "Avg Temp")]
   [app-comp/toggle
    {:value @(rf/subscribe [::subs/avg-corrected-temp-filter])
     :on-toggle #(rf/dispatch [::event/set-avg-corrected-temp-filter                               %])}]])

(defn sf-twt [{:keys [width height]}]
  (fn []
    (let [chambers (get-in @(rf/subscribe [::subs/config]) [:sf-config :chambers])
          item-width 800
          render-fn
          (fn [indexes show-item] 
            (map (fn [i]
                   (let [chart-row @(rf/subscribe [::subs/sf-twt-chart-row i])
                         chart-row (assoc chart-row
                                          :height ( - height 20)
                                          :width item-width)]
                     [twt-chart chart-row])) 
                 indexes))]
      [lazy-cols {:width width
                  :height height  
                  :item-width item-width
                  :items-render-fn render-fn
                  :item-count (count chambers)}])))

(defn tf-twt [{:keys [width height]} reduced-firing]
  (let [{:keys [tube-rows tube-row-count]}
        (:tf-config @(rf/subscribe [::subs/config]))
        item-width 800
        render-fn
        (fn [indexes show-item] 
          (map (fn [i]
                 (let [chart-row @(rf/subscribe [::subs/tf-twt-chart-row i])
                       chart-row (assoc chart-row
                                        :height ( - height 20)
                                        :width item-width)]
                   [twt-chart chart-row]))
               indexes))]
    [lazy-cols {:width width
                :height height  
                :item-width item-width
                :items-render-fn render-fn
                :item-count tube-row-count}]))

(defn twt-graph [{:keys [level], {:keys [width height]} :view-size}]
  (let [firing          @(rf/subscribe [::subs/firing])
        data-date @(rf/subscribe [::subs/data-date])
        twt-type        @(rf/subscribe [::subs/twt-temp]) 
        h1              125
        h3              48
        h2              (- height (+ h1  h3))
        style (style/body width height)]
    [:div
     [:div {:style {:height h1
                    :width width}}
      (let [twt-opts   @(rf/subscribe [::subs/twt-temp-opts])
            twt-type        @(rf/subscribe [::subs/twt-temp])
            selected (some #(if(= (:id %) twt-type) %) twt-opts)]     
        [:span (use-style style/form-field)
         [:span (use-sub-style style/form-field :label)
          (translate [:dataset :reduced-firing :label] "Choose type:")]
         [app-comp/selector {:item-width 70
                             :options twt-opts
                             :label-fn :label
                             :selected selected
                             :value-fn :id
                             :on-select #(rf/dispatch [::event/set-twt-temp
                                                       (% :id)])}]])
      [app-view/horizontal-line {:width (- width 20)}]
      
      [:div (use-style style/twt-graph-head)
       [:div (use-sub-style style/twt-graph-head :head)
        (translate [:dataset :tube-wall-temperature :label]
                   "Tube wall temperature")]
       [:div
        (use-sub-style style/twt-graph-head :sub-head)
        (let [{:keys [year month day hour minute]}
              (u/to-date-time-map data-date)]
          (format "%4d-%02d-%02d"
                  year month day hour minute))]]]
     [:div {:style {:height h2 :width width }}
      (case firing
        "side" [sf-twt  {:width width
                         :height h2}]
        "top" [tf-twt  {:width width
                        :height h2}])]
     [:div {:style {:height h3 :width width}}
      (reduced-firing)
      (avg-temp-band)
      (avg-raw-temp)
      (avg-corrected-temp)]]))
