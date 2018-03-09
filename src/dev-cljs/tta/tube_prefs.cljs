(ns tta.tube-prefs
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [tta.app.icon :as ic]
            [tta.app.style :as app-style]
            [ht.util.interop :as i]
            [tta.app.comp :as app-comp]
            [tta.app.subs :as app-subs]
            [tta.app.input :refer [tube-pref-list]]
            [tta.app.scroll :as scroll :refer [lazy-scroll-box
                                               lazy-list-cols
                                               scroll-box lazy-list-box]]))



(def my-container (r/atom {}))

(defn- tube-list-head [label on-clear]
  [app-comp/action-label-box {:width 280 ;; icon 24 & padding 32 takes 56
                              :label label
                              :right-icon ic/delete
                              :right-action on-clear
                              :right-disabled? (not on-clear)}])


(defn tube-number [start end ind]
  (if (> start end)
    (- start ind)
    (- end ind )))

(defn set-tube-pref []
  (js/console.log "set tube prefs"))

(defn clear-tube-prefs []
  (js/console.log "clear tube prefs"))

(defn- tube-prefs-component []
  (let []
    (r/create-class
     {:component-did-mount 
      (fn [this]
        (js/console.log this)
        (swap! my-container  assoc
               :width (i/oget-in this [:refs :container :offsetWidth])
               :height 400)) 
      :reagent-render
      (fn []
        [:div {:ref "container"}
         [tube-pref-list {:width (:width @my-container)
                          :height 400
                          :item-height 56
                          :item-width 250
                          :on-select #(set-tube-pref)
                          :on-clear #(clear-tube-prefs)
                          :rows  @(rf/subscribe [:ht.work.subs/tube-prefs])
                          :plant @(rf/subscribe [::app-subs/plant])}]])})))  

(defn tube-prefs []
  [tube-prefs-component])
