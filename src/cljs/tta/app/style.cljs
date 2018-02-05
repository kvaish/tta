(ns tta.app.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]))

(defonce do-once
  (do
    (stylefy/class "ht-ic-fill" {:fill "currentColor"})

    (stylefy/class "ht-ic-icon" {:fill "none !important"
                                 :stroke "currentColor"
                                 :stroke-width 1})

    ))

(def widget-transition {:std "450ms ease-in-out"})

(def widget-bg-d (color-hex :alumina-grey 30))
(def widget-bg-e (color-hex :sky-blue))
(def widget-fg (color-hex :white))

(defn toggle [on? enabled?]
  (let [widget-bg (if enabled? widget-bg-e widget-bg-d)]
    {:cursor (if enabled? "pointer")
     :width "48px"
     :height "20px"
     :border (str "1px solid " widget-bg)
     :border-radius "11px"
     :font-size "12px"
     :font-weight 300
     :line-height "20px"
     :position "relative"
     :background (if on? widget-bg widget-fg)
     :color (if on? widget-fg widget-bg)
     ;; :box-shadow (str "inset " (if on? widget-fg widget-bg-d) " -1px 1px 10px 1px")
     :transition (:std widget-transition)
     ::stylefy/sub-styles
     {:label (merge {:display "block"}
                    (if on? {:margin-left "10px"}
                        {:margin-right "10px"
                         :text-align "right"}))
      :circle (merge {:border-radius "50%"
                      :width "14px"
                      :height "14px"
                      :border (str "1px solid " widget-bg)
                      :position "absolute"
                      :top "2px"
                      :background widget-fg
                      ;; :box-shadow (str "inset " widget-bg-d " 3px 3px 3px 1px")
                      :transition (:std widget-transition)}
                     {:left (if on? "30px" "2px")})}}))

(def toggle-on (toggle true true))
(def toggle-off (toggle false true))
(def toggle-on-d (toggle true false))
(def toggle-off-d (toggle false false))

(def icon-button
  {::stylefy/sub-styles
   {:icon {:border-radius "50%"
           :background widget-bg-e
           :color (str widget-fg " !important")}}})

(def icon-button-disabled
  (assoc-in icon-button [::stylefy/sub-styles :icon :background]
            widget-bg-d))

(def icon-button-2
  (update-in icon-button [::stylefy/sub-styles :icon] assoc
             :width "22px !important"
             :height "22px !important"))

(def icon-button-2-disabled
  (assoc-in icon-button-2 [::stylefy/sub-styles :icon :background]
            widget-bg-d))

(defn selector [enabled?]
  (let [widget-bg (if enabled? widget-bg-e widget-bg-d)
        shadow-color (if enabled? (color-hex :sky-blue -20)
                         (color-hex :white -20))
        label {:position "absolute"
               :display "inline-block"
               :font-size "11px"
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
     {:marker {:background widget-bg
               :height "24px"
               :border-radius "12px", :min-width "24px"
               :transition (:std widget-transition)
               :position "absolute"
               ;; :box-shadow (str "inset " shadow-color " 3px 3px 10px 1px")
               :top "3px"}
      :label (if enabled? (assoc label :cursor "pointer") label)
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
