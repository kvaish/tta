(ns tta.style
  (:require [garden.def :refer [defstyles]]
            [garden.units :refer [px]]
            [garden.color :as gc]
            [ht.style :as ht]))

(defn color
  "get color suitable for use with stylefy and garden"
  ([color-key]
   (get ht/colors color-key))
  ([color-key pct-lighten]
   (-> (get ht/colors color-key)
       (gc/lighten pct-lighten))))

(defstyles app-styles

  [:div#app-loading {:position "fixed"
                     :width "100%"
                     :height "100%"}
   [:div#spinner {:margin "20% auto"
                  :width "100px"
                  :height "100px"
                  :background-image "url('../../images/hexagon_spinner.gif')"
                  :background-repeat "no-repeat"
                  :background-size "contain"}]]

  ;; remove outline from focus
  [:*:focus {:outline "none"}]

  ;; enforce border-box all
  [:* :*:before :*:after {:-webkit-box-sizing "border-box"
                          :-moz-box-sizing "border-box"
                          :box-sizing "border-box"}]

  [:div.my-custom-class
   {:color "red"}])
