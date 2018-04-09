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
    [:path {:d "M4,9 l1,-1 h14 l1,1 v8 l-1,1 h-14 l-1,-1 z"}]
    [:circle {:cx "12" :cy "13" :r "3"}]
    [:path {:d "M6,6 l2,-1 h2 l2,1"}]]))

(defn plus
  ([] (plus nil))
  ([props]
   [icon props
    [:path {:d "M12,5 v14"}]
    [:path {:d "M5,12 h14"}]]))

(defn minus
  ([] (minus nil))
  ([props]
   [icon props
    [:path {:d "M5,12 h14"}]]))

(defn dropdown
  ([] (dropdown nil))
  ([props]
   [icon props
    [:path {:d "M6,10 l6,6 l6,-6"}]]))

(defn nav-right
  ([] (nav-right nil))
  ([props]
   [icon props
    [:path {:d "M10,6 l6,6 l-6,6"}]]))

(defn nav-left
  ([] (nav-left nil))
  ([props]
   [icon props
    [:path {:d "M14,6 l-6,6 l6,6"}]]))

(defn gear
  ([] (gear nil))
  ([props]
   [icon props
    [:circle {:cx "12", :cy "12", :r "3.5"}]
    [:path {:d "M4,4 l"}]]))

(defn pyrometer+
  ([] (pyrometer+ nil))
  ([props]
   [icon props
    [:path {:d "M4,6 h16 v4 l-14,3 z"}]
    [:path {:d "M9,13 l-2,6 m-1,0 h6 m-1,0 l2,-6"}]
    [:path {:d "M18,13 v6 m-3,-3 h6"}]]))

(defn emissivity+
  ([] (emissivity+ nil))
  ([props]
   [icon props
    [:path {:d "M12,9 c-3,-2 -5,-2 -6,1 c0,3 5,3 5,3 m0,0 c0,0 -5,0 -5,3 c0,3 4,3 7,0"}]
    [:path {:d "M18,10 v6 m-3,-3 h6"}]]))

(defn mark-tube
  ([] (mark-tube nil))
  ([props]
   [icon props
    [:path {:d "M2,6 c 2,-4 2,-2 7,0 c -3,3 -5,4 -7,0 v18 m7,-18 v5
m0,13 v-10 c 3,-3 3,-3 7,0 c-3,4 -3,4 -7,0 m7,0 v10
m6,0 v-22 c -2,8 -5,3 -8,0 c 3,-3 3,-3 8,0 m-8,0 v8"}]]))

(defn save
  ([] (save nil))
  ([props]
   [icon props
    [:path {:d "M12,6 v8 l-3,-3 M12,14 l3,-3"}]
    [:path {:d "M5,12 v6 h14 v-6"}]]))

(defn upload
  ([] (upload nil))
  ([props]
   [icon props
    [:path {:d "M12,6 v8 M12,6 l-3,3 M12,6 l3,3"}]
    [:path {:d "M5,12 v6 h14 v-6"}]]))

(defn cancel
  ([] (cancel nil))
  ([props]
   [icon props
    [:path {:d "M6,6 l12,12 m0,-12 l-12,12"}]]))

(defn accept
  ([] (accept nil))
  ([props]
   [icon props
    [:path {:d "M3,12 l6,6 l12-12"}]]))

(defn delete
  ([] (delete nil))
  ([props]
   [icon props
    [:path {:d "M6,7 h12.5 m-5,0 v-2 h-2.5 v2"
            :fill "currentColor"}]
    [:path {:d "M7.5,6 v11 l1,1 h8 l1,-1 v-11"}]
    [:path {:d "M10,9 v6 m2.5,0 v-6 m2.5,0 v6"}]]))

(defn dataset
  ([] (dataset nil))
  ([props]
   [icon props
    [:rect {:x 5, :y 6 :width 14, :height 12}]
    [:path {:d "M6,7 h12 m0,3.3 h-12 m0,3.3 h12 m0,3.4 h-12
m4,0 v-12 m4,0 v12"}]]))

(defn report
  ([] (dataset nil))
  ([props]
   [icon props
    [:path {:d "M7,9 h3 v-3 h7 v12 h-10 v-9 l3,-3"}]
    [:path {:d "M9,11 h6 m0,2 h-6 m0,2 h6"}]]))

(defn datasheet
  ([] (dataset nil))
  ([props]
   [icon props
    [:path {:d "M7,9 h3 v-3 h7 v12 h-10 v-9 l3,-3"}]
    [:path {:d "M10,11 l5,5 m0,-5 l-5,5"}]]))

(defn license
  ([] (license nil))
  ([props]
   [icon props
    [:path {:d "M3,3 h15 v3 M3,3 v18 h8"}]
    [:path {:d "M5,10 h5 m-5,3 h5 m-5,3 h5"}]
    [:circle {:cx "17" :cy "12" :r "5"}]
    [:path {:d "M17,12 m-3,4 l-1,7 l4,-3 l4,3 l-1,-7"}]]))

(defn dataset-inadequate
  ([] (dataset-inadequate nil))
  ([props]
    [icon props
     [:path {:d "M12,20 a1,1 0 0,10 -16,0"
             :stroke "red" :stroke-width "2px"}]
     [:path {:d "M12,4 a1,1 0 0,10 16,0"
             :fill "red" :stroke "red" :stroke-width "2px"}]]))

(defn dataset-incomplete
  ([] (dataset-inadequate nil))
  ([props]
   [icon props
    [:path {:d "M12,20 a1,1 0 0,10 -16,0"
            :stroke "#002856" :stroke-width "2px"}]
    [:path {:d "M12,4 a1,1 0 0,10 16,0"
            :fill "#002856" :stroke "#002856" :stroke-width "2px"}]]))
