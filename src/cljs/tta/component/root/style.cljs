(ns tta.component.root.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
            [tta.app.style :as app-style]))

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
     :display "flex"
     :flex-direction "row"
     ::stylefy/vendors vendors
     ::stylefy/auto-prefix #{:flex-direction}
     ;; children styles
     ::stylefy/sub-styles
     {:left {:background-image "url('images/ht_logo_white.png')"
             :height (px logo-h)
             :background-repeat "no-repeat"
             :background-size "contain"
             :margin-top (px logo-s)
             :margin-left (px logo-s)
             :width "200px"}
      :middle {:flex 1
               ::stylefy/vendors vendors
               ::stylefy/auto-prefix #{:flex}}
      :right {:font-size "12px"
              :padding "24px 30px 0 12px"}
      :link {:text-decoration "none"
             :color (color :white)
             :margin-left "30px"}
      :link-icon {:margin-right "5px"}
      :icon-only {:font-size "18px"}}}))

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
                           :background-color (color :alumina-grey))
             :logo {:height "26px"
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
    {:height (px (- h 18))
     :padding "9px 0 9px 10px"
     ;; children styles
     ::stylefy/sub-styles
     {:link link
      :active-link (merge link {:font-weight 700})}}))

(def messages {}) ;; TODO: define style for warning and comment

(def info
  (let [{h :sub-head-row-height} ht/root-layout]
    {:height (px (- h 18))
     :padding "8px 0px 10px 30px"
     :overflow "hidden"
     :background (color :alumina-grey)
     :color (color :slate-grey -20)
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
