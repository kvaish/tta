;; styles for component dataset-selector
(ns tta.component.dataset-selector.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :refer [color-hex]]
            [tta.app.style :as app-style]))

(defn dataset-list-style []
  {:cursor "pointer"
   ::stylefy/sub-styles
           {:item {:cursor "pointer"
                   :width  "inherit"
                   :color       (color-hex :royal-blue)
                   ::stylefy/mode
                           {:hover
                            {:background-color (color-hex :alumina-grey)}}}
            :selected-item {:cursor "pointer"
                            :color  (color-hex :monet-pink)
                            ::stylefy/mode {:hover {:background-color "none"}}}
            :icon-style {:width   40
                         :display "inline-block"}
            :menu-item-style {:font-size   "12px"
                              :line-height "24px"
                              :min-height  "24px"}
            :display-date {:width       120
                           :font-size   "12px"
                           :line-height "24px"
                           :min-height  "24px"
                           :display     "inline-block"}}})