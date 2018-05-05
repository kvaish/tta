;; styles for component dataset
(ns tta.component.dataset.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [tta.app.style :as app-style]))

(defn twt-graph [width height]
  (let [h1 100
        h3 48
        h2 (- height h1 h3)]
    ^{:data {:h1 h1, :h2 h2, :h3 h3
             :w width}}
    {:width (px width), :height (px height)
     ::stylefy/sub-styles
     {:header {:width (px width), :height (px h1)}
      :label {:font-size "12px"
              :display "inline-block"
              :vertical-align "top"
              :padding "14px 0"
              :color (color-hex :royal-blue)}
      :title {:border-top (str "2px solid " (color-hex :slate-grey))
              :font-size "14px"
              :font-weight 700
              :color (color-hex :bitumen-grey)
              :padding "5px 0 0 0"}
      :sub-title {:color (color-hex :bitumen-grey)
                  :font-size "10px"}
      :body {:width (px width), :height (px h2)}
      :footer {:width (px width), :height (px h3)}}}))

(def sf-burner-table
  {::stylefy/sub-styles
   {:row-head {:text-align   "center"
               :line-height  "64px"
               :border-right "1px solid"
               :border-color (color-hex :sky-blue)
               :height       "inherit"}
    :col-head {:text-align    "center"
               :border-bottom "1px solid"
               :border-color  (color-hex :sky-blue)
               :height        "inherit"}
    :label {:display "inline-block"
            :color (color-hex :royal-blue)
            :font-size "12px"
            :vertical-align "top"
            :padding "14px 0px"}
    :title {:color (color-hex :royal-blue)
            :font-size "14px"
            :font-weight 700
            :text-align "center"}
    :body {:border (str "1px solid" (color-hex :sky-blue))
           :border-radius "6px"
           :margin "10px"
           :padding "20px"
           :position "relative"
           :user-select "none"}}})

(defn sf-burner-status-styles [bg-color]
  (let [col-on      (color-hex :green)
        col-off     (color-hex :red)
        col-off-aux (color-hex :orange)
        col-off-fix (color-hex :brown)
        front       {:width "24px", :height "24px"
                     :position "absolute"
                     :top "24px", :left   "24px"
                     :border-radius "50%"
                     :box-shadow "inset -6px -6px 10px rgba(0,0,0,0.3)"
                     :border (str "1px solid" bg-color)}
        back        (assoc front :left "14px" :top "15px")]
    {:cell  {:height     "64px", :width "64px"
             :border     (str "1px solid " (color-hex :white))
             :background bg-color
             :position   "relative"}
     :front {:on      (assoc front :background col-on)
             :off     (assoc front :background col-off)
             :off-aux (assoc front :background col-off-aux)
             :off-fix (assoc front :background col-off-fix)}
     :back  {:on      (assoc back :background col-on)
             :off     (assoc back :background col-off)
             :off-aux (assoc back :background col-off-aux)
             :off-fix (assoc back :background col-off-fix)}}))

(def sf-burner-style
  (let [col-on (color-hex :green)
        col-off (color-hex :red)
        col-off-aux (color-hex :orange)
        col-off-fix (color-hex :brown)
        circle {:width "24px", :height "24px"
                :position "absolute"
                :top "24px", :left "24px"
                :border-radius "50%"
                :background (color-hex :white)
                :box-shadow "inset -6px -6px 10px rgba(0,0,0,0.3)"}]
    {:height "48px", :width "48px"
     :display "inline-block"
     :position "relative"
     ::stylefy/sub-styles
     {:on (assoc circle :background col-on)
      :off (assoc circle :background col-off)
      :off-aux (assoc circle :background col-off-aux)
      :off-fix (assoc circle :background col-off-fix)
      :popup {:position "relative"
              :height "96px", :width "96px"
              :top "-12px", :left "-12px"
              :z-index "99999"
              :border-radius "50%"
              :background (color-rgba :white 0 0.5)
              :box-shadow "0 0 10px 3px rgba(0,0,0,0.3),
inset -3px -3px 10px rgba(0,0,0,0.3)"}
      :circle (assoc circle
                     :top "36px", :left "36px"
                     :box-shadow "-3px -3px 10px 3px rgba(0,0,0,0.3)")}}))


(defn overall-twt-graph [width height]
  (let [h1 48
        h3 48
        ;; footer (h3) will be overlayed on top of body (h2)
        h2 (- height h1)]
    ^{:data {:h1 h1, :h2 h2, :h3 h3
             :w width}}
    {:width (px width), :height (px height)
     :position "relative"
     ::stylefy/sub-styles
     {:header {:width (px width), :height (px h1)}
      :label {:font-size "12px"
              :display "inline-block"
              :vertical-align "top"
              :padding "14px 0"
              :color (color-hex :royal-blue)}
      :title {:border-top (str "2px solid " (color-hex :slate-grey))
              :font-size "14px"
              :font-weight 700
              :color (color-hex :bitumen-grey)
              :padding "5px 0 0 0"}
      :sub-title {:color (color-hex :bitumen-grey)
                  :font-size "10px"}
      :body {:width (px width), :height (px h2)}
      :footer {:width (px width), :height (px h3)
               :position "absolute"
               :bottom 0, :left 0}}}))
