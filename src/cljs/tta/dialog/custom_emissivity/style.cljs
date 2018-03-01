;; styles for dialog custom-emissivity
(ns tta.dialog.custom-emissivity.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [tta.app.style :as app-style]))



(def tube
  {:border-radius "100%"                ;
   :width "40px"
   :height "40px"
   :margin "auto"
   :border (str  "1px solid" (color-hex :sky-blue ))
   :color (color-hex :sky-blue )
   :display "inline-block"
   :line-height "40px"
   :text-align "center"
   :font-size "14px"
   })
