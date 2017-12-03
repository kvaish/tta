;; view elements component home
(ns tta.component.home.view
  (:require [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.util.common :refer [translate]]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.home.style :as style]
            [tta.component.home.subs :as subs]
            [tta.component.home.event :as event]))

(defn home [props]
  [:div (use-style style/home)
   [:div (use-sub-style style/home :item)
    "home"]])