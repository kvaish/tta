;; styles for component setting
(ns tta.component.settings.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
            [tta.app.style :as app-style]))

(def setting
  {:height "100%"
   :width "100%"
   :background-color (color-rgba :white)})

(def content-toolbar
  {:background-color (color-rgba :alumina-grey 65)})

(def toolbar-title
  {:font-size "18px"})

(def toolbar-icon
  {:font-size "20px"})

(def setting-container
  {:width "100%"
   :display "flex"
   :margin-top "10px"
   :flex-direction "row"
   :background-color (color-rgba :white)})

(def reformer-design-container
  {:height "100%"
   :flex 1
   :padding "20px"})

(def setting-form-container
  {:height "100%"
   :flex 1 
   :padding "20px"})

