;; styles for component config
(ns tta.component.config.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [tta.app.style :as app-style]))

(defn body [width height]
  (let [fs-h (- height 40)
        fs-w (* (- width 85) 0.5)
        f-w (- fs-w 5)
        c-w-1 (- f-w 5)
        c-w-2 (* 0.5 (- f-w 5))
        c-w-3 (* 0.33 (- f-w 5))
        c-w-4 (* 0.25 (- f-w 5))]
    {:width (px width), :height (px height)
     :padding "20px"
     :border (str "1px solid " app-style/widget-bg-e)
     :border-radius "8px"
     ::stylefy/sub-styles
     {:data {:f-w f-w, :f-h fs-h
             :c-w-1 c-w-1, :c-w-2 c-w-2
             :c-w-3 c-w-3, :c-w-4 c-w-4}
      :form-scroll {:height (px fs-h)
                    :width (px fs-w)
                    :display "inline-block"
                    :vertical-align "top"}
      :form {:width (px f-w)
             :padding "20px 0 0 0"}
      :form-cell-1 {:width (px c-w-1)
                  :vertical-align "top"
                  :display "inline-block"
                  :padding "0 0 6px 0"
                  :position "relative"}
      :form-cell-2 {:width (px c-w-2)
                    :vertical-align "top"
                    :display "inline-block"
                    :padding "0 0 6px 0"
                    :position "relative"}
      :form-cell-3 {:width (px c-w-3)
                    :vertical-align "top"
                    :display "inline-block"
                    :padding "0 0 6px 0"
                    :position "relative"}
      :form-cell-4 {:width (px c-w-4)
                    :vertical-align "top"
                    :display "inline-block"
                    :padding "0 0 6px 0"
                    :position "relative"}
      :form-cell-4x3 {:width (px (* 3 c-w-4))
                      :vertical-align "top"
                      :display "inline-block"
                      :padding "0 0 6px 0"
                      :position "relative"}
      :form-heading-label {:color (color-hex :royal-blue)
                           :user-select "none"
                           :font-size "14px"
                           :font-weight 400
                           :display "block"
                           :padding "14px 12px 0 12px"
                           :vertical-align "top"}
      :form-label {:color (color-hex :royal-blue)
                   :user-select "none"
                   :font-size "12px"
                   :font-weight 300
                   :display "inline-block"
                   :padding "14px 12px 0 12px"
                   :vertical-align "top"}
      :form-error {:color (color-hex :red)
                   :font-size "11px"
                   :display "block"
                   :position "absolute"
                   :bottom 0,
                   :left "12px"}
      :div-error {:color (color-hex :red)
                  :font-size "12px"
                  :display "block"
                  :margin "12px"}}}))
