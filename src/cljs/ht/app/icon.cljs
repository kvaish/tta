(ns ht.app.icon
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style]]
            [ht.app.style :as style]))


(defn close []
  [ui/svg-icon (use-style style/icon-plain)
   [:path {:d "M 6 6 l 12 12"}]
   [:path {:d "M 18 6 l -12 12"}]])
