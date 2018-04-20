;; styles for component dataset-selector
(ns tta.component.dataset-selector.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :refer [color-hex]]
            [tta.app.style :as app-style]))

(defn item-style []
  {:cursor "pointer"
   ::stylefy/sub-styles
   {:item {:cursor "pointer"}
    :selected {:cursor "pointer"
               :color (color-hex :monet-pink)
               :background-color (color-hex :gray)}}})