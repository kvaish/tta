(ns tta.app.view
  "collection of common small view elements for re-use"
  (:require [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as style]
            [tta.app.subs :as subs]
            [tta.app.event :as event]))


(defn layout-main [title sub-title actions body]
  (let [view-size @(rf/subscribe [::ht-subs/view-size])
        body-size (style/content-body-size view-size)
        style (style/layout-main view-size)]
    [:div (use-style style)
     [:div (use-sub-style style :head)
      [:div (use-sub-style style :head-left)
       [:span (use-sub-style style :title) title]
       [:span (use-sub-style style :sub-title) sub-title]]
      (into [:div (use-sub-style style :head-right)] actions)]
     [:div (use-sub-style style :body)
      [body body-size]]]))

(defn vertical-line [{:keys [height]}]
  [:div (use-style (style/vertical-line height))])
