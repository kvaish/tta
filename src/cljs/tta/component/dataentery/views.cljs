(ns tta.component.dataentry.views
  (:require [re-frame.core :as rf]
            [stylefy.core :refer [use-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.subs :as app-subs]
            [tta.app.events :as app-events]
            [tta.util.gen :as u]
            [tta.component.dataentry.style :as style]
            [tta.component.dataentry.subs :as subs]
            [tta.component.dataentry.events :as events]))

(defn dataentry-com []
  [:div "dataentry"])