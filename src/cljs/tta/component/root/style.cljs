(ns tta.component.root.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [tta.app.style :as app-style]))

(def header
  (let [{h :head-row-height} ht/root-layout
        logo-h 18
        logo-s (/ (- h logo-h) 2)]
    {:background           (:blue-spot-light ht/gradients)
     :height               (px h)
     :display              "flex"
     :flex-direction       "row"
     ::stylefy/vendors     vendors
     ::stylefy/auto-prefix #{:flex-direction}
     ;; children styles
     ::stylefy/sub-styles
     {:left   {:background-image  "url('images/ht_logo_white.png')"
               :height            (px logo-h)
               :background-repeat "no-repeat"
               :background-size   "contain"
               :margin-top        (px logo-s)
               :margin-left       (px logo-s)
               :width             "200px"}
      :middle {:flex                 1
               ::stylefy/vendors     vendors
               ::stylefy/auto-prefix #{:flex}}
      :right  {:font-size "12px"
               :padding   "24px 15px 0 12px"}
      :link {:text-decoration "none"
             :cursor "pointer"
             :display "inline-block"
             :height "24px"
             :padding "0 15px 0 0"
             :margin-left "15px"}
      :link-label {:color (color :white)
                   :font-size "12px"
                   :line-height "18px"
                   :display "inline-block"
                   :vertical-align "top"}}}))

(def sub-header
  (let [{h :sub-head-row-height} ht/root-layout
        col {:display "flex"
             :flex-direction "row"
             ::stylefy/auto-prefix #{:flex-direction :flex}
             ::stylefy/vendors vendors}]
    (merge col
           {:background-color (color :alumina-grey 50)
            :height (px h)
            ;;sub-styles
            ::stylefy/sub-styles
            {:left (assoc col :flex 3)
             :right (assoc col :flex 1
                           :background-color (color :alumina-grey 30))
             :logo {:height (px h)
                    :padding (px (/ (- h 26) 2))
                    :padding-left "20px"
                    :color (color :royal-blue)}
             :spacer (assoc col :flex 1)}})))

(def hot-links
  (let [{h :sub-head-row-height} ht/root-layout
        link {:text-decoration "none"
              :margin-left "20px"
              :font-size "12px"
              :color (color :royal-blue)}]
    {:height (px h)
     :padding "9px 0 9px 10px"
     ;; children styles
     ::stylefy/sub-styles
     {:link link
      :active-link (merge link {:font-weight 700})}}))

(def messages {}) ;; TODO: define style for warning and comment

(def info
  (let [{h :sub-head-row-height} ht/root-layout]
    {:height (px h)
     :padding "8px 0px 10px 30px"
     :overflow "hidden"
     :color (color :slate-grey)
     :font-size "12px"
     :flex 1
     ::stylefy/auto-prefix #{:flex}
     ::stylefy/vendors vendors
     ;; children styles
     ::stylefy/sub-styles
     {:p {:margin 0}
      :head {:font-weight 300
             :font-size "10px"
             :display "block"}
      :body {:line-height "12px"
             :display "block"}}}))

(def no-access
  {:padding "10% 15%"
   ::stylefy/sub-styles
   {:p {:font-size "18px"
        :font-weight 700
        :margin 0
        :color (color :red)}}})

(def content {:padding 0
              :background (color :slate-grey 20)})

(def root {:background-color (color :white)})
