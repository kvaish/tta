;; view elements dialog work
(ns ht.work.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [ht.app.icon :as ic]
            [ht.work.style :as style]
            [ht.work.subs :as subs]
            [ht.work.event :as event]
            [ht.app.comp :as ht-comp]
            [tta.icon-set]
            [tta.comp-set]
            [tta.scroll]
            [tta.ref-sketch]
            [tta.tube-list]
            [tta.tube-prefs]
            [tta.tab]))

(defn work []
  (if @(rf/subscribe [::subs/open?])
    [ui/dialog
     {:open true, :modal true
      :title (r/as-element
              (ht-comp/optional-dialog-head
               {:title "Workspace"
                :on-close #(rf/dispatch [::event/close])
                :close-tooltip "close"}))}
     (case (:work-key @(rf/subscribe [::subs/data]))
       :tta/icons [tta.icon-set/icon-set]
       :tta/comps [tta.comp-set/comp-set]
       :tta/scroll [tta.scroll/scroll-test]
       :tta/ref-sketch [tta.ref-sketch/ref-sketch]
       :tta/tube-list [tta.tube-list/tube-list]
       :tta/tube-prefs [tta.tube-prefs/tube-prefs]
       :tta/tab [tta.tab/tab]
       ;; default
       [:p "empty workspace"])]))
