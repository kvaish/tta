;; styles for dialog choose-plant
(ns tta.dialog.choose-plant.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
            [tta.app.style :as app-style]))




(def container
  {
   #_:width #_"100%"
   :height "auto"
   :min-height "200px"
   :max-height "400px"
  
   :margin-top "10px"
   :border "1px solid #e0e0e0"
   #_:border #_"1px solid grey"})

(def selected-client
  {:height "60px"
   :margin-top "10px"
   :font-size "14px"
   :padding "10px"
   :color (color :white)
   :background (color :royal-blue)
   :display ""
   ::stylefy/sub-styles
   { :button {
              :display "inline"
              :float "right"
              :position "absolute"
              :right "30px"
              :bottom "140px"}}})
