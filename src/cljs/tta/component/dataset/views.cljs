(ns tta.component.dataset.views
  (:require [re-frame.core :as rf]
            [stylefy.core :refer [use-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.subs :as app-subs]
            [tta.app.events :as app-events]
            [tta.util.gen :as u]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.events :as events]))

(defn dataset-com []
  [:div "datasetpage"])