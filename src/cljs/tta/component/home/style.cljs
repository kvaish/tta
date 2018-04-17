;; styles for component home
(ns tta.component.home.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [tta.app.style :as app-style]))

(def home
  {:height "100%"
   :background "url('images/background.jpg')"
   :background-size "cover"
   :display "flex"
   :flex-direction "column"
   ::stylefy/auto-prefix #{:flex-direction}
   ::stylefy/vendors vendors
   ::stylefy/sub-styles
   {:primary-row {:flex 6
                  :margin "20px 0 20px 20px"
                  :display "flex"
                  :flex-direction "row"
                  ::stylefy/vendors vendors
                  ::stylefy/auto-prefix #{:flex :flex-direction}}
    :secondary-row {:flex 4
                    :margin "0 0 20px 20px"
                    :display "flex"
                    :flex-direction "row"
                    ::stylefy/vendors vendors
                    ::stylefy/auto-prefix #{:flex :flex-direction}}}})

(def card-base
  {:background-color (str (color-rgba :white nil 0.6) " !important")
   :user-select "none"
   :border-radius "10px !important"
   :-ms-transition-duration "100ms !important"
   ::stylefy/vendors vendors
   ::stylefy/auto-prefix #{:flex :transform}})

(def card-with-buttons
  {:flex 1
   :flex-direction "column"
   :display "flex"
   ::stylefy/vendors vendors
   ::stylefy/auto-prefix #{:flex :flex-direction}})

(def card-button
  (merge card-base
         {:font-size "22px"
          :margin "10px 20px 0 0"
          :padding "15px 30px"
          :height "60px"
          ::stylefy/sub-styles {:title {:font-size "22px"
                                        :color (color :royal-blue)}}}))

(defn set-clickable [style]
  (merge style
         {:cursor "pointer"
          ::stylefy/mode {:hover
                          {:background (str (color-rgba :white nil 0.9)
                                            " !important")
                           :box-shadow "rgba(0, 0, 0, 0.12) 10px 8px 6px, rgba(0, 0, 0, 0.12) 10px 8px 4px !important"
                           :transform "translate(-5px, -5px)"}}}))

(defn disable-card [style]
  (assoc style
         :background-color (str (color-rgba :alumina-grey nil 0.8)
                                " !important")))

(defn disable-button [style]
  (assoc style :color (color-hex :slate-grey)))

(def card-primary
  (merge card-base
         {:flex 1
          :position "relative"
          :margin "0 20px 0 0"
          :padding "30px"
          ::stylefy/sub-styles
          {:title {:font-weight 600
                   :font-size "28px"
                   :color (color :royal-blue)}
           :hr {:display "block"
                :float "left"
                :width "20px"
                :margin-top 0
                :border (str "1px solid " (color-hex :royal-blue))}
           :icon {:height "84px"
                  :width "84px"
                  :position "absolute"
                  :top "60px"
                  :right "48px"}
           :desc {:color (color :royal-blue)
                  :max-width "250px"
                  :margin-right "20px"
                  :font-weight 300
                  :position "absolute"
                  :bottom "30px"
                  :font-size "22px"}}}))

(def card-secondary
  (let [ss ::stylefy/sub-styles]
    (-> card-primary
        (assoc :padding "20px")
        (assoc-in [ss :title :font-size] "18px")
        (update-in [ss :icon] merge
                   {:height "60px"
                    :width "60px"
                    :top "48px"
                    :right "24px"})
        (update-in [ss :hr] merge
                   {:width "24px"
                    :border-width "0.5px"
                    :-ms-border-width "1px"})
        (update-in [ss :desc] merge {:font-size "14px"
                                     :position "absolute"
                                     :bottom "20px"}))))
