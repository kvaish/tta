(ns tta.tube-list
  (:require [reagent.core :as r]
            [tta.app.input :refer [list-tube-both-sides]]))

(defonce tube-count 100)

(defn create-tube-data []
  {:sides (vec (repeat 2 {:tubes (vec (repeat tube-count {:temp nil}))}))})

(defonce state (r/atom {:tube-data (create-tube-data)}))

(defn tube-field [index side]
  {:value (get-in @state [:tube-data :sides side :tubes index :temp])
   :valid? true})

(defn set-tube-field [index side value]
  (swap! state assoc-in [:tube-data :sides side :tubes index :temp] value))

(defn clear-tubes []
  (swap! state assoc :tube-data (create-tube-data)))

(defn tube-pref [index]
  (if (#{1 10 25} index) "imp"
      (if (#{4 15 21} index) "pin")))

(defn tube-list []
  [:div {:style {:height 320 :display "inline-block"}}
   [list-tube-both-sides
    {:label      "Chamber 1"
     :height     300
     :start-tube 1
     :end-tube   tube-count
     :field-fn   tube-field
     :pref-fn    tube-pref
     :on-change  set-tube-field
     :on-clear   (if (some (fn [side]
                             (some :temp (:tubes side)))
                           (get-in @state [:tube-data :sides]))
                   clear-tubes)}]])
