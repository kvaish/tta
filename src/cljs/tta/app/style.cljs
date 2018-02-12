(ns tta.app.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]))

(defonce do-once
  (do
    (stylefy/class "ht-ic-fill" {:fill "currentColor"})

    (stylefy/class "ht-ic-icon" {:fill "none"
                                 :stroke "currentColor"
                                 :stroke-width 1
                                 :display "inline-block"
                                 :user-select "none"
                                 :width "24px"
                                 :height "24px"})

    ))

(def widget-transition {:std "450ms ease-in-out"})

(def widget-bg-d (color-hex :alumina-grey 30))
(def widget-bg-e (color-hex :sky-blue))
(def widget-bg-h (color-hex :sky-blue 20))
(def widget-fg (color-hex :white))

;; 72x48
(defn toggle [on? disabled?]
  (let [widget-bg (if disabled? widget-bg-d widget-bg-e)]
    {:cursor (if-not disabled? "pointer")
     :width "46px"
     :height "20px"
     :border (str "1px solid " widget-bg)
     :border-radius "11px"
     :font-size "12px"
     :font-weight 300
     :line-height "20px"
     :position "relative"
     :background (if on? widget-bg widget-fg)
     :color (if on? widget-fg widget-bg)
     :transition (:std widget-transition)
     ::stylefy/sub-styles
     {:container {:display "inline-block"
                  :padding "13px 12px"
                  :vertical-align "top"}
      :label {:display "block"
              :margin "0 10px 0 10px"
              :text-align (if on? "left" "right")}
      :circle {:border-radius "50%"
               :width "12px"
               :height "12px"
               :border (str "1px solid " widget-bg)
               :position "absolute"
               :top "3px"
               :background widget-fg
               :left (if on? "30px" "3px")
               :transition (:std widget-transition)}}}))

;; 48x48, icon: 24x24
(defn icon-button [disabled?]
  {:border-radius "50%"
   :background (if disabled? widget-bg-d widget-bg-e)
   :color widget-fg})

;; *x48, icon: 24x24
(defn button [disabled?]
  {:bg (if disabled? widget-bg-d widget-bg-e)
   :fg widget-fg
   :hc widget-bg-h
   :btn {:border-radius "16px"
         :height "32px"
         :margin "8px 12px"
         :color widget-fg}
   :div {:height "24px"
         :padding "4px 24px"
         :color widget-fg}
   :icon {:color widget-fg}
   :span {:display "inline-block"
          :vertical-align "top"
          :height "24px"
          :line-height "24px"
          :font-size "12px"
          :margin-left "12px"
          :color widget-fg}})

(defn selector [disabled?]
  (let [widget-bg (if disabled? widget-bg-d widget-bg-e)
        label {:position "absolute"
               :display "inline-block"
               :font-size "12px"
               :font-weight 300
               :text-align "center"
               :top 0
               :padding "6px 0"
               :transition (:std widget-transition)
               :color widget-bg}]
    {:border (str "1px solid " widget-bg)
     :height "30px"
     :border-radius "15px", :min-width "30px"
     :position "relative"
     ::stylefy/sub-styles
     {:container {:display "inline-block"
                  :padding "8px 12px"
                  :vertical-align "top"}
      :marker {:background widget-bg
               :height "24px"
               :border-radius "12px", :min-width "24px"
               :transition (:std widget-transition)
               :position "absolute"
               :top "3px"}
      :label (if disabled? label (assoc label :cursor "pointer"))
      :active-label (assoc label :color widget-fg)}}))

(def scroll-bar
  {::stylefy/sub-styles
   {:bar-h {:position "absolute"
            :cursor "pointer"
            :left "3px"
            :bottom 0
            :height "9px"}
    :bar-v {:position "absolute"
            :cursor "pointer"
            :top "3px"
            :right 0
            :width "9px"}
    :line-h {:position "absolute"
             :background (color :alumina-grey -20)
             :bottom "4px"
             :left 0
             :height "1px"}
    :line-v {:position "absolute"
             :background (color :alumina-grey -20)
             :right "4px"
             :top 0
             :width "1px"}
    :track-h {:position "absolute"
              :bottom 0
              :height "3px"
              :padding "3px 0"
              :cursor "ew-resize"
              ::stylefy/mode {:hover {:height "7px"
                                      :padding "1px 0"}}}
    :track-v {:position "absolute"
              :right 0
              :width "3px"
              :padding "0 3px"
              :cursor "ns-resize"
              ::stylefy/mode {:hover {:width "7px"
                                      :padding "0 1px"}}}
    :track {:background (color :sky-blue)
            :border-radius "4px"
            :width "100%"
            :height "100%"}}})
