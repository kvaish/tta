(ns tta.component.logger.views
   (:require [re-frame.core :as rf]
             [stylefy.core :refer [use-style]]
             [cljs-react-material-ui.reagent :as ui]
             [tta.app.subs :as app-subs]
             [tta.app.events :as app-events]
             [tta.util.gen :as u]
             [tta.component.logger.style :as style]
             [tta.component.logger.subs :as subs]
             [tta.component.logger.events :as events]))

(defn logger-com []
  [:div
"logger"
   ;[ui/dialog {:title "logger" :model {true} :open {true}}]

   ]
)