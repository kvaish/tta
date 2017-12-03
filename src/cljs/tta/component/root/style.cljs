(ns tta.component.root.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [tta.app.style :as app-style
             :refer [color color-hex color-rgba]]))

(defn content-height [view-size]
  (let [{:keys [head-row-height sub-head-row-height]} ht/root-layout]
    (- (:height view-size)
       head-row-height
       sub-head-row-height)))

(def header
  (let [{h :head-row-height} ht/root-layout
        logo-h 18
        logo-s (/ (- h logo-h) 2)]
    {:background (:blue-spot-light ht/gradients)
     :height (px h)
     :position "relative"
     ;; children styles
     ::stylefy/sub-styles
     {:logo {:background-image "url('images/ht_logo_white.png')"
             :height (px logo-h)
             :background-repeat "no-repeat"
             :background-size "contain"
             :top (px logo-s)
             :left (px logo-s)
             :width "33%"
             :position "absolute"}
      :right {:float "right"
              :font-size "12px"
              :padding "24px 30px 12px 12px"}
      :link {:text-decoration "none"
             :color (color :white)
             :margin-left "30px"}
      :link-icon {:margin-right "5px"}
      :icon-only {:font-size "18px"}}}))

(def sub-header
  (let [{h :sub-head-row-height} ht/root-layout]
    {:background-color (color :alumina-grey 80)
     :height (px h)
     :color (color :royal-blue)
     :position "relative"}))

(def sub-header-left
  (let [{h :sub-head-row-height} ht/root-layout]
    {:position "relative"
     :display "inline-block"
     :height (px (- h 24))
     :padding "12px 0 12px 30px"} ))

(def sub-header-middle
  (let [{h :sub-head-row-height} ht/root-layout
        link {:text-decoration "none"
              :margin-left "50px"
              :font-size "14px"
              :color (color :royal-blue)}]
    (merge sub-header-left {:padding "12px 0 12px 50px"}
           {;; children styles
            ::stylefy/sub-styles
            {:link link
             :active-link (merge link {:font-weight 700
                                       :font-size "16px"})}})))

(def sub-header-right
  (let [{h :sub-head-row-height} ht/root-layout]
    (merge sub-header-left {:height (px (- h 18))
                            :padding "8px 0px 10px 30px"
                            :overflow "hidden"
                            :max-width "25%"
                            :min-width "12%"
                            :float "right"
                            :background (color :alumina-grey 20)
                            :color (color :slate-grey)
                            :font-size "12px"}
           {;; children styles
            ::stylefy/sub-styles
            {:info-p {:margin 0}
             :info-head {:font-weight 300
                         :font-size "10px"
                         :line-height "12px"}
             :info-body {}}})))

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
