(ns tta.tube-list
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [tta.app.icon :as ic]
            [tta.app.input :as app-inp]))

(defn create-data [tube-count]
  {:sides (vec (repeat 2 {:tubes (vec (repeat tube-count {:temp nil}))}))})

(defonce state (r/atom {:start-tube 101
                        :end-tube 200
                        :data (create-data 100)}))

(defn field [index side]
  {:value (get-in @state [:data :sides side :tubes index :temp])
   :valid? true})

(defn set-field [index side value]
  (swap! state assoc-in [:data :sides side :tubes index :temp] value))

(defn clear []
  (swap! state assoc :data (create-data 100)))

(defn pref [index]
  (if (#{1 10 25} index) "imp"
      (if (#{4 15 21} index) "pin")))

(defn tube-list []
  (let [{:keys [start-tube end-tube data]} @state
        on-clear (if (some (fn [side]
                             (some :temp (:tubes side)))
                           (:sides data))
                   clear)]
    [:div {:style {:height 320}}
     [app-inp/tube-list {:label "Chamber 1"
                         :height 300
                         :start-tube start-tube
                         :end-tube end-tube
                         :field-fn field
                         :pref-fn pref
                         :on-change set-field
                         :on-clear on-clear}]]))
