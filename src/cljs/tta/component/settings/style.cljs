;; styles for component setting
(ns tta.component.settings.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [tta.app.style :as app-style]))

(defn body [width height]
  (let [fs-h (- height 40)
        fs-w (* (- width 85) 0.4)
        f-w (- fs-w 5)
        c-w (- (* 0.5 f-w) 5)
        c-w-2 (- f-w 5)]
    {:width (px width), :height (px height)
     :padding "20px"
     :border (str "1px solid " app-style/widget-bg-e)
     :border-radius "8px"
     ::stylefy/sub-styles
     {:data {:f-w f-w, :f-h fs-h, :c-w c-w, :c-w-2 c-w-2}
      :form-scroll {:height (px fs-h)
                    :width (px fs-w)
                    :display "inline-block"
                    :vertical-align "top"}
      :form {:width (px f-w)
             :padding "20px 0 0 0"}
      :form-cell {:width (px c-w)
                  :vertical-align "top"
                  :display "inline-block"
                  :padding "0 0 6px 0"
                  :position "relative"}
      :form-cell-2 {:width (px c-w-2)
                    :vertical-align "top"
                    :display "inline-block"
                    :padding "0 0 6px 0"
                    :position "relative"}
      :form-label {:color (color-hex :royal-blue)
                   :font-size "12px"
                   :font-weight 300
                   :display "inline-block"
                   :padding "14px 12px 0 12px"
                   :vertical-align "top"}
      :form-error {:color (color-hex :red)
                   :font-size "11px"
                   :display "block"
                   :position "absolute"
                   :bottom 0, :left "12px"}}}))
