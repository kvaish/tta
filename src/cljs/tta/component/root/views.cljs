(ns tta.component.root.views
  (:require [re-frame.core :as rf]
            [stylefy.core :refer [use-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.subs :as app-subs]
            [tta.app.events :as app-events]
            [tta.util.gen :as u]
            [tta.component.root.style :as style]
            [tta.component.root.subs :as subs]
            [tta.component.root.events :as events]))

(defn top-bar []
  [:div (use-style style/top-bar)
   "Top bar"])

(defn menu-bar []
  [:div (use-style style/menu-bar)
   "Menu bar"])

(defn main-container []
  (let [view-size @(rf/subscribe [::app-subs/view-size])]
    [:div (update (use-style style/main-container) :style
                  assoc :height (style/main-container-height view-size))
     (u/translate [:main :greet :label] "Hi")
     " Main container"
     [:br]
     [ui/flat-button {:label "Set language"
                      :secondary true
                      :on-click #(rf/dispatch [::app-events/set-language :ru])}]]))

(defn root []
  [:div (use-style style/root)
   [top-bar]
   [menu-bar]
   [main-container]])


