(ns tta.component.root.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [tta.app.style :refer [color]]
            [goog.object :as g]))

(def config {:top-bar-h 66
             :menu-bar-h 50})

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
   :background "radial-gradient(circle farthest-side at 60% 400%,rgba(84,201,233,1),rgba(0,72,187,1)100%)"
   :height (px (:top-bar-h config))
   :color (color :white)
   :font-size "12px"
   ::stylefy/mode {:hover {:color (color :white)}}})

(def menu-bar
  {:background-color "#f5f5f5"
   :height (px (:menu-bar-h config))
   :color (color :bitumen-grey)})

(def menu-bar-right
  {:background-color (color :alumina-grey 10)
   :height "100%"
   :color (color :slate-grey)
   :display "inline-block"
   :padding-right "5%"
   })


(def main-container
  {:background-color (color :slate-grey)
   :background-image "url('images/background.jpg')"
   :background-repeat "no-repeat"
   :background-size "cover"})

(def top-bar-logo
  {:background-image "url('images/ht_logo_white.png')"
   :height "18px"
   :background-repeat "no-repeat"
   :background-size "contain"
   :top "24px"
   :left "24px"
   :width "50%"
   :position "relative"})

(def top-bar-menu-link
  {:padding "8px 24px 0px 30px"
   :display "inline-block"
   :text-decoration "none"
   :color (color :white)
   })
(def menu-bar-spacing
  {:padding  "0 3% 0 0"
   :text-decoration "none"
   :color  (color :royal-blue)
   ::stylefy/mode {:active {:color (color :royal-blue)}}})
(def menu-bar-active
  {:color (color :royal-blue)
   :font-weight "600"})

(def pull-left
  {:float "left"})

(def pull-right
     {:float "right" })



(def root
  {:background-color (color :white)})
