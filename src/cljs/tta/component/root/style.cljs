(ns tta.component.root.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [tta.app.style :refer [color]]
            [goog.object :as g]))

(def config {:top-bar-h 66
             :menu-bar-h 40})

(defn main-container-height [view-size]
  (- (:height view-size)
     (:top-bar-h config)
     (:menu-bar-h config)))



(defn color-rgb [color-key]
  (let [{:keys [red green blue]} (gc/as-rgb (color color-key))]
    (str "rgba(" red "," green "," blue ",1)")))

;; logo height: 18px
;; spacing top/bottom: 24px
;; total height: 66px
(def top-bar
  {;:background "linear-gradient(-90deg, rgba(57,135,192, 1), rgba(85,165,203, 1) 30%, rgba(25, 99, 182, 1))"
   ;:background "radial-gradient(farthest-side at 70% 200%, rgba(85,165,203, 1),rgba(25, 99, 182, 1) 200%)"
   :background "radial-gradient(circle farthest-side at 60% 400%,rgba(84,201,233,1),rgba(0,72,187,1)150%)"
   :height (px (:top-bar-h config))
   ::stylefy/mode {:hover {:color (color :white)}}})

(def menu-bar
  {:background-color (color :alumina-grey)
   :height (px (:menu-bar-h config))})

(def main-container
  {:background-color (color :slate-grey)})

(def root
  {:background-color (color :bitumen-grey)})
