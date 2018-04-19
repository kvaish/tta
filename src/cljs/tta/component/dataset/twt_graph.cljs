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

(defn form-cell-4 [style error label widget]
  [:div (use-sub-style style :form-cell-4)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error)
    (if (fn? error) (error) error)]])

(defn twt-type [style]) 

(defn reduced-firing [style]
  (let [rf-opts @(rf/subscribe [::subs/on-off-opts])
        rf @(rf/subscribe [::subs/reduced-firing])
        selected (some #(if(= (:id %) rf) %) rf-opts)]

    [form-cell-4 style nil
     (translate [:dataset :reduced-firing :label] "Reduced Firing")
     [app-comp/selector {:item-width 40
                         :options rf-opts
                         :label-fn :label
                         :selected selected
                         :value-fn :id
                         :on-select #(rf/dispatch [::event/set-reduced-firing
                                                   (% :id)])}]]))

(defn avg-temp-band [style]
  (let [at-opts @(rf/subscribe [::subs/on-off-opts])
        rf @(rf/subscribe [::subs/avg-temp-band])
        selected (some #(if(= (:id %) rf) %) at-opts)]

    [form-cell-4 style nil
     (translate [:dataset :avg-temp-band :label] "+/- 20° off Avg")
     [app-comp/selector {:item-width 40
                         :options at-opts
                         :label-fn :label
                         :selected selected
                         :value-fn :id
                         :on-select #(rf/dispatch [::event/set-avg-temp-band
                                                   (% :id)])}]]))

(defn avg-raw-temp [style]
  (let [act-opts @(rf/subscribe [::subs/on-off-opts])
        rf @(rf/subscribe [::subs/avg-raw-temp])
        selected (some #(if(= (:id %) rf) %) act-opts)]

    [form-cell-4 style nil
     (translate [:dataset :avg-raw-temp :label] "Avg. Raw Temp")
     [app-comp/selector {:item-width 40
                         :options act-opts
                         :label-fn :label
                         :selected selected
                         :value-fn :id
                         :on-select #(rf/dispatch [::event/set-avg-raw-temp
                                                   (% :id)])}]]))

(defn avg-corrected-temp [style]
  (let [act-opts @(rf/subscribe [::subs/on-off-opts])
        rf @(rf/subscribe [::subs/avg-corrected-temp])
        selected (some #(if(= (:id %) rf) %) act-opts)]

    [form-cell-4 style nil
     (translate [:dataset :avg-corrected-temp :label] "Avg. Corr. Temp")
     [app-comp/selector {:item-width 40
                         :options act-opts
                         :label-fn :label
                         :selected selected
                         :value-fn :id
                         :on-select #(rf/dispatch [::event/set-avg-corrected-temp
                                                   (% :id)])}]]))


(defn sf-twt [{:keys [width height]}]
  (let [chambers (get-in @(rf/subscribe [::subs/config]) [:sf-config :chambers])
        item-width 600
        item-count (count chambers)
        item-width (if (> (+ width 10)
                          (* item-width item-count))
                     width item-width)
        render-fn
        (fn [indexes show-item] 
          (map (fn [i]
                 [:div {:style {:height height
                                :width item-width
                                :border "1px solid grey"}}])
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
                 [:div {:style {:height height
                                :width item-width
                                :border "1px solid grey"}}])
               indexes))]
    [lazy-cols {:width width
                :height height  
                :item-width item-width
                :items-render-fn render-fn
                :item-count tube-row-count}]))
 
(defn twt-graph [{:keys [level], {:keys [width height]} :view-size}]
  (let [firing          @(rf/subscribe [::subs/firing])
        twt-type        @(rf/subscribe [::subs/twt-type])
        h1              55
        h3              55
        h2              (- height (+ h1  h3))
        style (style/body width height)]
    [:div
     [:div {:style {:height h1
                    :font-size "14px"
                    :width width
                    :border "1px solid pink"}}
      (let [twt-opts   @(rf/subscribe [::subs/twt-type-opts])
            twt-type        @(rf/subscribe [::subs/twt-type])
            selected (some #(if(= (:id %) twt-type) %) twt-opts)]

        [form-cell-2 style nil
         (translate [:dataset :reduced-firing :label] "Choose type:")
         [app-comp/selector {:item-width 70
                             :options twt-opts
                             :label-fn :label
                             :selected selected
                             :value-fn :id
                             :on-select #(rf/dispatch [::event/set-twt-type
                                                       (% :id)])}]])]
     
     [:div {:style {:height h2 :width width :border "1px solid red"}}
      (case firing
        "side" [sf-twt  {:width width
                              :height h2}]
        "top" [tf-twt  {:width width
                             :height h2}])]
     [:div {:style {:height h3 :width width :border "1px solid green"}}
      (reduced-firing style)
      (avg-temp-band style)
      (avg-raw-temp style)
      (avg-corrected-temp style)]]))
