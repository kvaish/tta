;; styles for component dataset-selector
(ns tta.component.dataset-selector.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :refer [color-hex]]
            [tta.app.style :as app-style]))

(defn dataset-list-style []
  (let [w 220
        iw (- w 20)]
    ^{:data {:w w}}
    {:display "inline-block"
     ::stylefy/sub-styles
     {:item {:margin "0 10px"
             :width (px iw)
             :cursor "pointer"
             :color (color-hex :royal-blue)
             ::stylefy/mode {:hover {:background-color (color-hex :alumina-grey)}}}
      :selected-item {:width (px iw)
                      :margin "0 10px"
                      :color  (color-hex :monet-pink)
                      ::stylefy/mode {:hover {:background-color "none"}}}
      :display-date {:width "120px"
                     :font-size "12px"
                     :line-height "24px"
                     :min-height "24px"
                     :display "inline-block"
                     :vertical-align "top"}}}))
