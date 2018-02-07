(ns tta.comp-set
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]))

(rf/reg-sub
 ::value
 (fn [db [_ path]]
   (get-in db path)))

(defn comp-set []
  [:div
   [:div
    [app-comp/toggle {:path [:work :comp-set :check1]}]
    [app-comp/toggle
     {:path [:work :comp-set :check2]
      :disabled? (not (:value @(rf/subscribe [::value
                                              [:work :comp-set :check1]])))}]

    ;; [:span {:style {:width "100px"}}]
    [app-comp/icon-button-s {:icon ic/plus}]
    [app-comp/icon-button-s {:icon ic/minus}]
    [app-comp/icon-button-s {:icon ic/plus, :disabled? true}]
    [app-comp/icon-button-s {:icon ic/minus, :disabled? true}]]
   [:div
    [app-comp/toggle {:path [:work :comp-set :check1]}]
    [app-comp/icon-button {:icon ic/camera}]
    [app-comp/selector {:path [:work :comp-set :selector1]
                        :options ["Wall" "Burner" "Tube"]
                        :item-width 70}]
    [app-comp/selector
     {:path [:word :comp-set :selector2]
      :options ["Data" "Graph"]
      :disabled? (not (:value @(rf/subscribe [::value
                                              [:work :comp-set :check2]])))
      :item-width 70}]]])
