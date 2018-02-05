(ns ht.app.comp
  (:require [re-frame.cofx :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.app.style :as style]
            [ht.app.icon :as ic]))

(defn optional-dialog-head
  "create title comp for non-modal dialogs, with a close button"
  [{:keys [title on-close close-tooltip]}]
  [:div (use-style style/optional-dialog-head)
   [:span (use-sub-style style/optional-dialog-head :title) title]
   [ui/icon-button (merge (use-sub-style style/optional-dialog-head :close)
                          {:on-click on-close
                           :tooltip close-tooltip})
    [ic/close]]])
