(ns tta.component.dataset.twt-entry.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.input :refer [list-tube-both-sides]]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.view :as app-view]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]))

(defn interactive-design [props]
  (let [{:keys [level view-size]} props
        width (/ (:width view-size) 2)]
    [:div {:style {:float "left"
                   :width width
                   :height (:height view-size)
                   :border "1px solid red"}}]))
(defn tube-pref-fn [index]
  (let [prefs @(rf/subscribe [::subs/tube-prefs])]))

(defn tube-field-fn [index side]
  (let [level (or @(rf/subscribe [::subs/selected-level]) 0)]
    @(rf/subscribe [::subs/tf-dataset-tube level index side])))

(defn tube-wall-entry-component [props]
  (let [{:keys [tube-row-count tube-rows]} @(rf/subscribe [::subs/tf-config])
        ]
    (r/create-class
     {:component-did-mount
      (fn [this]   (js/console.log "did mount"))
      :reagent-render
      (fn [props]
        (let [{:keys [level view-size]} props
              height (:height view-size)
              width (:width view-size)]
          (js/console.log props)
          [:div {:style {:width 300
                         :height (- height 20) 
                         :border "1px solid pink"}}
           
           (let [render-fn
                 (fn [indexes show-item]
                   (map (fn [i]
                          (let [{:keys [name tube-count
                                        start-tube end-tube]}
                                (get tube-rows i)]
                            (js/console.log (get tube-rows i))
                            [list-tube-both-sides
                             {:label name
                              :height 30
                              :start-tube start-tube
                              :end-tube end-tube
                              :field-fn tube-field-fn
                              :pref-fn tube-pref-fn
                              :on-change #(js/console.log "change")}]))
                        indexes))]
             
             [lazy-cols {:width 300
                         :height height
                         :item-width 300
                         :item-count tube-row-count
                         :items-render-fn render-fn}])]))})))


(defn tube-wall-entry [props]
  (let [{:keys [level view-size]} props
        width (/ (:width view-size) 2)]
    [:div {:style {:float "right"
                   :width width
                   :padding 10
                   :height (:height view-size)
                   :border "1px solid green"}}
     
     [tube-wall-entry-component props]]))

(defn top-twt-entry [props]
  (let [{:keys [level view-size]} props
        width (/ (:width view-size) 2)]
    [:div 
     (interactive-design props)
     (tube-wall-entry props)]))

(defn side-twt-entry [props] 
  [:div "side"])

(defn twt-entry [props]
  (let [firing @(rf/subscribe [::subs/firing])]
    (case firing
      "top" [top-twt-entry props]
      "side" [side-twt-entry props])))
