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

(defn close-icon []
  [ui/svg-icon (use-style style/close-icon)
   [:path {:d "M 6 6 l 12 12"}]
   [:path {:d "M 18 6 l -12 12"}]])

(defn optional-dialog-head [props]
  (let [{:keys [title on-close close-tooltip]} props]
    [:div (use-style style/optional-dialog-head)
     [:span (use-sub-style style/optional-dialog-head :title) title]
     [ui/icon-button (merge (use-sub-style style/optional-dialog-head :close)
                            {:on-click on-close
                             :tooltip close-tooltip})
      [close-icon]]]))
