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
     [:path {:d "M12,20 a1,1 0,0,1 0,-16"}]
     [:path {:d "M12,4 a1,1 0,0,1 0,16"
             :fill "currentColor"}]]))

(defn dataset-incomplete
  ([] (dataset-incomplete nil))
  ([props]
   [icon props
    [:path {:d "M12,20 a1,1 0,0,1 0,-16"}]
    [:path {:d "M12,4 a1,1 0,0,1 0,16"
            :fill "currentColor"}]]))

(defn menu
  ([] (menu nil))
  ([props]
   [icon props
    [:path {:d "M6,8 h12 m0,4 h-12 m0,4 h12"}]]))

(defn reset
  ([] (reset nil))
  ([props]
   [icon props
    [:path {:d "M6,9 a 7,7 0 1 1 0,6"}]
    [:path {:d "M6,3 v6 h6"}]]))


;; more complex icons ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; fixed view-box     ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn icon-complex [props view-box & children]
  (into [:svg (-> (assoc props :view-box view-box)
                  (update :class str " ht-ic-icon"))]
        children))

(defn gear
  ([] (gear nil))
  ([props]
   [icon-complex props "-64 -64 640 640"
    ;; original view-box "0 0 512 512" ;; source font-awesome - cog
    [:path {:d "M444.788 291.1l42.616 24.599c4.867 2.809 7.126 8.618 5.459 13.985-11.07 35.642-29.97 67.842-54.689 94.586a12.016 12.016 0 0 1-14.832 2.254l-42.584-24.595a191.577 191.577 0 0 1-60.759 35.13v49.182a12.01 12.01 0 0 1-9.377 11.718c-34.956 7.85-72.499 8.256-109.219.007-5.49-1.233-9.403-6.096-9.403-11.723v-49.184a191.555 191.555 0 0 1-60.759-35.13l-42.584 24.595a12.016 12.016 0 0 1-14.832-2.254c-24.718-26.744-43.619-58.944-54.689-94.586-1.667-5.366.592-11.175 5.459-13.985L67.212 291.1a193.48 193.48 0 0 1 0-70.199l-42.616-24.599c-4.867-2.809-7.126-8.618-5.459-13.985 11.07-35.642 29.97-67.842 54.689-94.586a12.016 12.016 0 0 1 14.832-2.254l42.584 24.595a191.577 191.577 0 0 1 60.759-35.13V25.759a12.01 12.01 0 0 1 9.377-11.718c34.956-7.85 72.499-8.256 109.219-.007 5.49 1.233 9.403 6.096 9.403 11.723v49.184a191.555 191.555 0 0 1 60.759 35.13l42.584-24.595a12.016 12.016 0 0 1 14.832 2.254c24.718 26.744 43.619 58.944 54.689 94.586 1.667 5.366-.592 11.175-5.459 13.985L444.788 220.9a193.485 193.485 0 0 1 0 70.2zM336 256c0-44.112-35.888-80-80-80s-80 35.888-80 80 35.888 80 80 80 80-35.888 80-80z"
            :stroke-width "25"}]]))

(defn magnify
  ([] (magnify nil))
  ([props]
   [icon-complex props "-64 -64 640 640"
    ;; original view-box "0 0 512 512" ;; source font-awesome - search
    [:path {:d "M505 442.7L405.3 343c-4.5-4.5-10.6-7-17-7H372c27.6-35.3 44-79.7 44-128C416 93.1 322.9 0 208 0S0 93.1 0 208s93.1 208 208 208c48.3 0 92.7-16.4 128-44v16.3c0 6.4 2.5 12.5 7 17l99.7 99.7c9.4 9.4 24.6 9.4 33.9 0l28.3-28.3 c9.4-9.4 9.4-24.6.1-34z"
            :stroke-width "25"}]]))
