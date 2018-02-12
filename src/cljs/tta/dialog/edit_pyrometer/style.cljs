;; styles for dialog edit-pyrometer
(ns tta.dialog.edit-pyrometer.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
            [tta.app.style :as app-style]))

(def row
  {:display "flex"
   :padding "8px 2px 8px 2px"})
(def col
  {:flex 1}  )
