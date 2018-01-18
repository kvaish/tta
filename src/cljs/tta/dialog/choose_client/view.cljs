;; view elements dialog choose-client
(ns tta.dialog.choose-client.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.dialog.choose-client.style :as style]
            [tta.dialog.choose-client.subs :as subs]
            [tta.dialog.choose-client.event :as event]))

(defn choose-client []
  (let [open? @(rf/subscribe [::subs/open?])]
    [ui/dialog
     {:open open?
      :modal true}
     [:div
      "choose-client"]]))