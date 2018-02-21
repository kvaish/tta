;; styles for dialog edit-pyrometer
(ns tta.dialog.edit-pyrometer.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
            [tta.app.style :as app-style]))

(def form-field
  {:display "inline-block"
   :position "relative"
   :padding "0 0 8px 12px"
   ::stylefy/sub-styles
   {:label {:font-size "12px"
            :font-weight 300
            :margin-top "14px"
            :color (color-hex :royal-blue)
            :vertical-align "top"
            :display "inline-block"}
    :error {:position "absolute"
            :color app-style/widget-err
            :font-size "10px"
            :bottom 0, :left "12px"}}})

(def menu-item
  {:font-size "14px"
   :line-height "24px"
   :padding "8px"
   :border-radius "8px"
   :margin-bottom "8px"
   :position "relative"})

(def edit
  {:padding "12px"
   :border (str "1px solid " app-style/widget-bg-e)
   :border-radius "8px"
   :position "relative"
   ::stylefy/sub-styles
   {:btns {:position "absolute"
           :top "12px", :right "12px"}}})

(def body
  {:position "relative"
   :padding-bottom "60px"
   ::stylefy/sub-styles
   {:btns {:position "absolute"
           :bottom 0, :right 0}}})
