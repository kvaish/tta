;; styles for component home
(ns tta.component.home.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [tta.app.style :as app-style
             :refer [color color-hex color-rgba vendors]]))

(def home
  {:height "100%"
   :background "url('images/background.jpg')"
   :background-size "cover"
   :background-repeat "no-repeat"
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
   :root {:display "block"}})

(defn set-cursor [style pointer?]
  (assoc style :cursor (if pointer? "pointer")))

(defn disable-card [style]
  (assoc style :background-color (str (color-hex :alumina-grey) " !important")
         :cursor nil))

(defn disable-button [style]
  (assoc style :color (color-hex :slate-grey)))

(def card-primary
  {:flex 1
   :position "relative"
   :margin "0 20px 0 0"
   :padding "30px"
   :opacity 0.8
   ::stylefy/vendors vendors
   ::stylefy/auto-prefix #{:flex}
   ::stylefy/sub-styles
   {:title {:font-weight 700
            :font-size "24px"
            :color (color :royal-blue)}
    :hr {:width "30px"
         :margin-left 0
         :margin-top 0
         :border (str "1.5px solid" (color-hex :royal-blue))}
    :icon {:color (color :royal-blue)
           :width "100px"
           :position "absolute"
           :bottom "30px"
           :right 0}
    :desc {:color (color :royal-blue)
           :max-width "200px"
           :font-size "18px"}}})

(def card-secondary
  (let [ss ::stylefy/sub-styles]
    (-> card-primary
        (assoc :padding "20px")
        (assoc-in [ss :title :font-size] "20px")
        (assoc-in [ss :hr :width] "24px")
        (assoc-in [ss :hr :border-width] "1px")
        (update-in [ss :desc] merge { :font-size "16px"
                                     :position "absolute"
                                     :bottom "20px"}))))
