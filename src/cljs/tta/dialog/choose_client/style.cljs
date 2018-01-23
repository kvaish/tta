;; styles for dialog choose-client
(ns tta.dialog.choose-client.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
            [tta.app.style :as app-style]))


(def select-client-container
  {:padding "10px"
   :width "100%"
   :height "auto"
   :display "flex"
   :flex-direction "column"
   ::stylefy/sub-styles
   {:p {:font-size "18px"
        :margin-top "10px"
        :color (color :green)}
    :input-type {:text {:backdrop-color "green"}}
    }})


(def select-client-filters
  {:padding "0px"
   :width "100%"
   :height "auto"
   #_:border #_"1px solid grey"
   :display "inline-block"})

(def results-container
  {
   :width "100%"
   :height "auto"
   :min-height "200px"
   :max-height "400px"
   :overflow-y "auto"
   :margin-top "10px"
   :border "1px solid #e0e0e0"
   #_:border #_"1px solid grey"})

(def results-header
  {:width "100%"
   :height "auto"
   :min-height "30px"
   :padding "8px"
   :text-align "middle"})

(def filter-fields
  {:max-width "150px"
   :margin "10px"})

(def filter-results
  {:height "20px"
   ::stylefy/sub-styles
   {:ul {:list-style-type "none"}
    :li {:border-bottom "1px solid #e0e0e0"
         :min-height "45px"
         :font-size "14px"
         :line-height "0.7"
         :cursor "pointer"}}})

(def filter-results-busy
  {:height "100%"
   :width "100%"
   :position "absolute"
   :border "1px solid red"
   :background-size "cover"
   :background "url('images/hexagon_spinner.gif') no-repeat"
   })

(def selected-client
  {:height "100px"
 
   :margin-top "10px"
   :font-size "14px"
   :padding "10px"
   :color (color :white)
   :background (color :royal-blue)
   ::stylefy/sub-styles
   { :button {:float "right"
              :right "10px"}}})
