(ns tta.app.icon
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [tta.app.style :as style]))

(defn plant
  ([] (plant nil))
  ([props]
   [ui/svg-icon (merge {:class-name "ht-ic-icon"} props)
    [:path {:d "M0,0 h6 v14 l9,-8 v8 l8,-8 v17 h-23z"}]]))

(defn my-apps
  ([] (my-apps nil))
  ([props]
   [ui/svg-icon (merge {:class-name "ht-ic-icon"} props)
    [:path {:d "M3,3 h6 v6 h-6 z", :class-name "ht-ic-fill"}]
    [:path {:d "M15,3 h6 v6 h-6 z"}]
    [:path {:d "M3,15 h6 v6 h-6 z"}]
    [:path {:d "M15,15 h6 v6 h-6 z"}]]))

(defn logout
  ([] (logout nil))
  ([props]
   [ui/svg-icon (merge {:class-name "ht-ic-icon"} props)
    [:circle {:cx "12" :cy "12" :r "10"}]
    [:path {:d "M9,7 a 6,6 0 1 0 6,0"}]
    [:path {:d "M12,13 v-8"}]]))

(defn plus-2
  ([] (plus-2 nil))
  ([props]
   [ui/svg-icon (merge {:class-name "ht-ic-icon"
                        :view-box "0 0 22 22"} props)
    [:path {:d "M11,4 v14", :shape-rendering "crispEdges"}]
    [:path {:d "M4,11 h14", :shape-rendering "crispEdges"}]]))

(defn minus-2
  ([] (minus-2 nil))
  ([props]
   [ui/svg-icon (merge {:class-name "ht-ic-icon"
                        :view-box "0 0 22 22"} props)
    [:path {:d "M4,11 h14", :shape-rendering "crispEdges"}]]))
