(ns ht.app.view
  "collection of common small view elements for re-use"
  (:require [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as style]
            [ht.app.subs :as subs :refer [translate]]
            [ht.app.event :as event]))

(defn busy-screen []
  (let [busy? @(rf/subscribe [::subs/busy?])
        {:keys [content paper spinner]} style/busy-screen]
    [ui/dialog
     {:open busy?
      :modal true
      :contentStyle content
      :paperProps {:style paper}}
     [:div (use-style spinner)]]))
