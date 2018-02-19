(ns tta.app.style
  (:require [stylefy.core :as stylefy]
            [garden.units :refer [px]]
            [garden.color :as gc]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]))

(defn content-height [view-size]
  (let [{:keys [head-row-height sub-head-row-height]} ht/root-layout]
    (- (:height view-size)
       head-row-height
       sub-head-row-height)))

(def head-content-height 72)

(defn content-body-size [view-size]
  (let [h (content-height view-size)]
    {:height (- h head-content-height 20)
     :width (- (:width view-size) 40)}))

(defonce do-once
  (do
    (stylefy/class "ht-ic-fill" {:fill "currentColor"})

    (stylefy/class "ht-ic-icon" {:fill "none"
                                 :stroke "currentColor"
                                 :stroke-width 1
                                 :display "inline-block"
                                 :user-select "none"
                                 :width "24px", :height "24px"})

    ))

(def widget-transition {:std "450ms ease-in-out"})

(def widget-bg-d (color-hex :alumina-grey 30))
(def widget-bg-e (color-hex :sky-blue))
(def widget-bg-h (color-hex :sky-blue 20))
(def widget-fg (color-hex :white))
(def widget-err (color-hex :red))

;; 72x48
(defn toggle [on? disabled?]
  (let [widget-bg (if disabled? widget-bg-d widget-bg-e)]
    {:display "inline-block"
     :padding "13px 12px"
     :vertical-align "top"
     :width "72px", :height "48px"
     ::stylefy/sub-styles
     {:main {:cursor (if-not disabled? "pointer")
                  :width "48px", :height "22px"
                  :border (str "1px solid " widget-bg)
                  :border-radius "11px"
                  :position "relative"
                  :background (if on? widget-bg widget-fg)
                  :color (if on? widget-fg widget-bg)
                  :transition (:std widget-transition)}
      :label {:display "block"
              :font-size "12px"
              :font-weight 300
              :margin "0 10px 0 10px"
              :text-align (if on? "left" "right")}
      :circle {:border-radius "50%"
               :width "14px", :height "14px"
               :border (if-not on? (str "1px solid " widget-bg))
               :position "absolute"
               :top "3px"
               :background widget-fg
               :left (if on? "29px" "4px")
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
   :container {:display "inline-block"
               :height "48px"
               :padding "8px 12px"}
   :btn {:border-radius "16px"
         :height "32px"
         :color widget-fg}
   :div {:height "24px"
         :padding "0 24px 0 12px"
         :color widget-fg}
   :icon {:color widget-fg}
   :span {:display "inline-block"
          :vertical-align "top"
          :height "24px"
          :line-height "24px"
          :font-size "12px"
          :margin-left "12px"
          :color widget-fg}})

;; *x48
(defn selector [disabled? valid?]
  (let [widget-bg (if disabled? widget-bg-d widget-bg-e)
        label {:position "absolute"
               :display "inline-block"
               :overflow "hidden"
               :font-size "12px"
               :height "24px"
               :line-height "20px"
               :font-weight 300
               :text-align "center"
               :top "4px"
               :transition (:std widget-transition)
               :color widget-bg}]
    {:display "inline-block"
     :height "48px"
     :padding "8px 12px"
     :vertical-align "top"
     ::stylefy/sub-styles
     {:main {:border (str "1px solid " (if valid? widget-bg widget-err))
             :height "32px"
             :border-radius "16px", :min-width "32px"
             :position "relative"}
      :marker {:background widget-bg
               :height "24px"
               :border-radius "12px", :min-width "24px"
               :transition (:std widget-transition)
               :position "absolute"
               :top "3px"}
      :label (if disabled? label (assoc label :cursor "pointer"))
      :active-label (assoc label :color widget-fg)}}))

;; (120+)*x48
(defn text-input [read-only? valid?]
  {:display "inline-block"
   :padding "8px 12px"
   :vertical-align "top"
   ::stylefy/sub-styles
   {:main {:height "32px"
           :border (str "1px solid " (if valid? widget-bg-e widget-err))
           :border-radius "16px"
           :min-width "32px"
           :padding "0 12px"
           :font-size "12px"
           :color (if valid? widget-bg-e widget-err)
           :background-color widget-fg}}})

;; (120+)x48
(defn action-input-box [disabled? valid? action? left-disabled? right-disabled?]
  (let [widget-bg (if disabled? widget-bg-d widget-bg-e)
        left-disabled? (or disabled? left-disabled?)
        right-disabled? (or disabled? right-disabled?)
        icon {:vertical-align "top"
              :border-radius "50%"
              :height "24px"
              :color widget-fg}
        icon-e {:cursor "pointer"
                :background widget-bg-e
                ::stylefy/mode
                {:hover {:background (color-hex :sky-blue 20)}}}]
    {:display "inline-block"
     :padding "8px 12px"
     :vertical-align "top"
     ::stylefy/sub-styles
     {:main {:height "32px"
             :border (str "1px solid " (if valid? widget-bg widget-err))
             :border-radius "16px"
             :padding "3px 4px"
             :background-color widget-fg}
      :span {:display "inline-block"
             :overflow "hidden"
             :vertical-align "top"
             :font-size "12px"
             :color (if valid? widget-bg widget-err)
             :height "24px"
             :padding "0 12px"
             :line-height "24px"
             :min-width "62px"
             :cursor (if (and action? (not disabled?)) "pointer")}
      :left (merge icon
                   (if left-disabled?
                     {:background widget-bg-d}
                     icon-e))
      :right (merge icon
                    (if right-disabled?
                      {:background widget-bg-d}
                      icon-e))}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
              :height "9px"
              :padding "3px 0"
              :cursor "ew-resize"
              ::stylefy/mode {:hover {:padding "1px 0"}}}
    :track-v {:position "absolute"
              :right 0
              :width "9px"
              :padding "0 3px"
              :cursor "ns-resize"
              ::stylefy/mode {:hover {:padding "0 1px"}}}
    :track {:background (color :sky-blue)
            :border-radius "4px"
            :width "100%"
            :height "100%"}}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn vertical-line [height]
  {:margin "20px"
   :border "none"
   :border-left (str "1px solid " widget-bg-e)
   :width "0"
   :height (px (- height 40))
   :display "inline-block"})

(defn layout-main [view-size]
  (let [w (:width view-size)
        h (content-height view-size)
        {bh :height, bw :width} (content-body-size view-size)]
    {:height (px h)
     :width (px w)
     :background (color :alumina-grey 70)
     ::stylefy/sub-styles
     {:head {:height (px head-content-height)
             :width (px w)
             :display "flex"}
      :head-left {:flex 1
                  :color (color :royal-blue)
                  :padding "14px 20px"
                  :font-weight 700}
      :title {:display "block"
              :font-size "16px"}
      :sub-title {:display "block"
                  :font-size "10px"}
      :head-right {:padding "12px 24px"}
      :body {:height (px bh)
             :width (px bw)
             :margin "0 20px 20px 20px"}}}))
