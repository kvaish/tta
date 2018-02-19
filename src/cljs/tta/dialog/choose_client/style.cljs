;; styles for dialog choose-client
(ns tta.dialog.choose-client.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
            [tta.app.style :as app-style]))

(def flex-col {:display "flex"
               :flex-direction "column"
               ::stylefy/auto-prefix #{:flex-direction :flex}
               ::stylefy/vendors vendors})

(def flex-row (assoc flex-col :flex-direction "row"))

(def toggle-field {:margin-top "20px !important"})

(def progress-bar {:margin-bottom "10px !important"})

(def container
  (-> flex-row
      (merge {:width "100%", :height "100%"
              :min-height "100px"})
      (assoc ::stylefy/sub-styles
             {:left (assoc flex-col :margin-right "20px")
              :right (assoc flex-col :flex 1
                            :border-left (str "1px solid "
                                              (color-hex :alumina-grey)))})))

(defn client-selector [view-size]
  {:overflow-y "auto"
   :max-height (px (- (:height view-size) 300))})
