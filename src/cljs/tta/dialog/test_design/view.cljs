;; view elements dialog test-design
(ns tta.dialog.test-design.view
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
            [tta.dialog.test-design.style :as style]
            [tta.dialog.test-design.subs :as subs]
            [tta.dialog.test-design.event :as event]
            [tta.component.reformer-layout.view :refer [reformer-layout reformer-data]]
            [tta.component.reformer-layout-tf.view :refer [reformer-layout-tf reformer-data-tf]]))


(defn test-design []
  (let [open? @(rf/subscribe [::subs/open?])]
    [ui/dialog
     {:open  true
      :modal true}
     [:div {:style {:width 500 :height 500}}
      ;[reformer-layout {:reformer-data @reformer-data}]
      [reformer-layout-tf
       (let [width (+ 50 (* 100 (get-in @reformer-data-tf [:configuration :tf-config :tube-row-count]))) ]
         {:reformer-data @reformer-data-tf
          :view-box      (str "0 0 " width " 500")
          :svg-width width
          :height 500
          })]
      ]]))