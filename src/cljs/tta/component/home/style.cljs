;; styles for component home
(ns tta.component.home.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [tta.app.style :as app-style
             :refer [color color-hex color-rgba]]))

(def home {:color (color :royal-blue)
            ::stylefy/sub-styles
            {:item {:color (color :white)}}})