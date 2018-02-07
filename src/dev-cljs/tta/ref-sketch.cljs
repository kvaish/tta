(ns tta.ref-sketch
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [tta.component.reformer-layout.view :refer [reformer-layout]]))

(defn ref-sketch []
  [:div {:style {:padding "50px"
                 :background "lightblue"}}
   [reformer-layout {:width "600px" :height "500px"}]])
