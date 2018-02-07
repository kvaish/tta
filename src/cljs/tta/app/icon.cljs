(ns tta.app.icon
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [tta.app.style :as style]))

(defn icon [props & children]
  (into [:svg (-> (merge {:view-box "0 0 24 24"} props)
                  (update :class str " ht-ic-icon"))]
        children))

(defn plant
  ([] (plant nil))
  ([props]
   [icon props
    [:path {:d "M1,1 h6 v14 l9,-8 v8 l7,-8 v16 h-22z"}]]))

(defn my-apps
  ([] (my-apps nil))
  ([props]
   [icon props
    [:path {:d "M3,3 h6 v6 h-6 z", :class "ht-ic-fill"}]
    [:path {:d "M15,3 h6 v6 h-6 z"}]
    [:path {:d "M3,15 h6 v6 h-6 z"}]
    [:path {:d "M15,15 h6 v6 h-6 z"}]]))

(defn logout
  ([] (logout nil))
  ([props]
   [icon props
    [:circle {:cx "12" :cy "12" :r "10"}]
    [:path {:d "M9,7 a 6,6 0 1 0 6,0"}]
    [:path {:d "M12,13 v-8"}]]))

(defn camera
  ([] (camera nil))
  ([props]
   [icon props
    [:path {:d "M4,7 l1,-1 h14 l1,1 v10 l-1,1 h-14 l-1,-1 z"}]
    [:circle {:cx "12" :cy "12" :r "3.5"}]
    [:path {:d "M6,5 l2,-1 h2 l2,1"}]]))

(defn plus
  ([] (plus nil))
  ([props]
   [icon props
    [:path {:d "M12,5 v14"            }]
    [:path {:d "M5,12 h14"}]]))

(defn minus
  ([] (minus nil))
  ([props]
   [icon props
    [:path {:d "M5,12 h14"}]]))
