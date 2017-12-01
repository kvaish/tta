(ns tta.component.trendline.views
  (:require [re-frame.core :as rf]
            [stylefy.core :refer [use-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.subs :as app-subs]
            [tta.app.events :as app-events]
            [tta.util.gen :as u]
            [tta.component.trendline.style :as style]
            [tta.component.trendline.subs :as subs]
            [tta.component.trendline.events :as events]))



(defn trendline-com []
  [:div "trendline"])