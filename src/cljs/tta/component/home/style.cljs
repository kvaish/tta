;; styles for component home
(ns tta.component.home.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
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

(def card-button
  {:label {:text-transform "none"
           :font-size "18px"
           :color (color :royal-blue)}
   :root {:display "block"
          :margin "0 0 5px 0"}})

(defn set-clickable [style pointer?]
  (merge style
         (if pointer?
           {:cursor "pointer"
            ::stylefy/mode {:hover
                            {:background (str (color-rgba :white nil 0.9)
                                              " !important")
                             :box-shadow "rgba(0, 0, 0, 0.12) 10px 8px 6px, rgba(0, 0, 0, 0.12) 10px 8px 4px !important"
                             :transform "translate(-5px, -5px)"}}}
           {:background (color-rgba :white nil 0.8)})))

(defn disable-card [style]
  (assoc style
         :background-color (str (color-rgba :alumina-grey nil 0.8)
                                " !important")))

(defn disable-button [style]
  (assoc style :color (color-hex :slate-grey)))

(def card-primary
  {:flex 1
   :position "relative"
   :margin "0 20px 0 0"
   :padding "30px"
   :background-color (str (color-rgba :white nil 0.6) " !important")
   :border-radius "10px !important"
   :-ms-transition-duration "100ms !important"
   ::stylefy/vendors vendors
   ::stylefy/auto-prefix #{:flex :transform}
   ::stylefy/sub-styles
   {:title {:font-weight 600
            :font-size "24px"
            :color (color :royal-blue)}
    :hr {:display "block"
         :float "left"
         :width "30px"
         :margin-top 0
         :border (str "1px solid " (color-hex :royal-blue))}
    :icon {:color (color :royal-blue)
           :width "100px"
           :position "absolute"
           :bottom "30px"
           :right 0}
    :desc {:color (color :royal-blue)
           :max-width "260px"
           :font-weight 300
           :font-size "18px"}}})

(def card-secondary
  (let [ss ::stylefy/sub-styles]
    (-> card-primary
        (assoc :padding "20px")
        (assoc-in [ss :title :font-size] "20px")
        (update-in [ss :hr] merge
                   {:width "24px"
                    :border-width "0.5px"
                    :-ms-border-width "1px"})
        (update-in [ss :desc] merge { :font-size "16px"
                                     :position "absolute"
                                     :bottom "20px"}))))
