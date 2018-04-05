(ns tta.table-grid
  (:require [reagent.core :as r]
        [reagent.dom :as dom]
        [tta.app.scroll :refer [lazy-scroll-box scroll-box table-grid]]))



(defn table-grid-test []
  (let [my-state (r/atom {:text "content"
                          :height 300, :width 200})]
    (fn []
      [:div
       [table-grid {
           :height 400, :width 600
           :row-header-width 50, :col-header-height 30
           :row-count 50 :col-count 50
           :row-height 30 :col-width 50
           :table-count [2 1]
           :gutter [5 5]
           :labels ["R" "C"]
           :padding [3 3 5 5]
           :row-header-renderer (fn [rowno table]
                [:div (str "R" rowno)]
            )
           :col-header-renderer (fn [colno table]
                [:div (str colno)]
            )
           :cell-renderer (fn [rowno colno table]
                [:div (str rowno "|" colno)]
            )
        ;    :corner-renderer (fn [table] 
        ;         "R/C"
        ;     )
       }]
       ])))
