;; styles for component config
(ns tta.component.config.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
            [tta.app.style :as app-style]))

(def menu
  {:width "100%"
   :height "auto"
   :border "1px solid red"})

(def container
  {:width "100%"
   :height "90%"
   :display "flex"})

(def sketch
  {:width "35%"
   :height "auto"
   :flex-direction "row"})

(def form
  {:width "65%"
   :height "auto"
   :flex-direction "row"
   :margin "0px 10px"})