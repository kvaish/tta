;; styles for dialog view-factor
(ns tta.dialog.view-factor.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [tta.app.style :as app-style]))


(def form-field
  {:display "inline-block"
   :position "relative"
   :float "right"
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
