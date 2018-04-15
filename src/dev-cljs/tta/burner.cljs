(ns tta.burner
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.style :refer [color color-hex color-rgba]]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :as scroll]
            [tta.component.dataset.burner-entry :refer [tf-burner-table
                                                        sf-burner-table]]))

(defonce my-state (r/atom {}))

(defn burner-tf-comp []
  [tf-burner-table
   500 300 10 7
   #(js/console.log %1 %2)
   #(js/console.log %1 %2)
   #(js/console.log %1 %2 %3) 400])

(defn burner-sf-comp []
  [sf-burner-table
   700 300 10 15
   #(js/console.log %1 %2 %3)
   #(js/console.log %1 %2 %3 %4) 400])

(defn burner []
  #_[:div {:style {:height 300 :padding "50px"}}
   [color-palette]
   [:div
    {:style {:float "left"}}
    [sf-burner {:value     (get-in @my-state [:sf :burner])
                :on-change #(swap! my-state assoc-in [:sf :burner] %)}]

    [sf-burner {:value        (get-in @my-state [:sf :burner])
                :on-change    #(swap! my-state assoc-in [:sf :burner] %)
                :dual-nozzle? true}] [:br]

    [tf-burner {:value     (get-in @my-state [:tf :burner])
                :on-change #(swap! my-state assoc-in [:tf :burner] %)}]]]
  #_[burner-tf-comp]

  [burner-sf-comp])
