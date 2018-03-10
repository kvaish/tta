(ns tta.wall-list
  (:require [reagent.core :as r]
            [tta.app.input :refer [list-wall-temps]]))

(defonce wall-count 5)

(defonce state (r/atom {:wall-temps (vec (repeat wall-count nil))}))

(defn wall-temp [index]
  {:value (get-in @state [:wall-temps index])
   :valid? true})

(defn set-wall-temp [index value]
  (swap! state assoc-in [:wall-temps index] value))

(defn clear-wall-temps []
  (swap! state assoc :wall-temps (vec (repeat wall-count nil))))

(defn add-wall-temps-row []
  (swap! state update :wall-temps conj nil))

(defn wall-list []
  (let [{:keys [wall-temps]} @state]
    [:div {:style {:height 320 :display "inline-block"}}
     [list-wall-temps
      {:label      "Chamber 1"
       :height     300
       :wall-count (count wall-temps)
       :on-clear   (if (some some? wall-temps)
                     clear-wall-temps)
       :field-fn   wall-temp
       :on-add     add-wall-temps-row
       :on-change  set-wall-temp}]]))
