(ns tta.app.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]))

(def close-icon {:fill "none !important"
                 :stroke (color-rgba :black 0 0.87)
                 :stroke-width 0.5})

(def optional-dialog-head
  {:position "relative"
   :margin 0
   :padding 0
   ::stylefy/sub-styles
   {:title {:display "block"}
    :close {:position "absolute !important"
            :top 0
            :right 0}}})
