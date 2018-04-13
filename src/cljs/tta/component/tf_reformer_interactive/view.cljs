;; view elements component tf-reformer-interactive
(ns tta.component.tf-reformer-interactive.view
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
    ;;DOTO subscribe selected measure level
    ;[tta.component.dataset.subs :as dataset-subs]
            [tta.app.d3 :refer [d3-svg get-value d3-svg-2-string]]
            [tta.component.tf-reformer-interactive.style :as style]
            [tta.component.tf-reformer-interactive.subs :as subs]
            [tta.component.tf-reformer-interactive.event :as event]
            [cljsjs.d3]
            [ht.util.interop :as i]))

(defn label-attr
  ([x y] (label-attr x y false nil))
  ([x y v?] (label-attr x y v? nil))
  ([x y v? a]
   {:stroke "none", :fill "white"
    :x x, :y y
    :transform #(if (get-value v? %1 %2 %3)
                  (let [x (get-value x %1 %2 %3)
                        y (get-value y %1 %2 %3)]
                    (str "rotate(270," x "," y ")")))
    :text-anchor a}))

(def top-fired-nodes
  (let [w 600, h 400, m 80, p 20
        x m, y m, w (- w m m), h (- h m m)
        xe (+ x w), ye (+ y h)
        xm (+ x (/ w 2)), ym (+ y (/ h 2))
        y1 (+ y p), y2 (- ye p)
        ;;DOTO assign selected measure level
        ;;@(rf/subscribe [::dataset-subs/measure-level-selected])
        measure-level "top"
        ]
    [{:tag   :g, :class :reformer
      :nodes [;; chamber box
              {:tag  :rect, :class :ch-box
               :attr {:x      x, :y y,
                      :rx     "6px" :ry " 6px"
                      :width  w, :height h
                      :stroke "#484b4e", :stroke-width "3px"}}
              ;; section label
              #_{:tag  :text, :class :s-label
                 :attr (label-attr (- x 6) (+ y 6) true "end")
                 :text #(str (get-in % [:tf-config :section-count]) " Sections")}
              ;; wall label group
              {:tag   :g :class :w-label-box
               :data  (fn [c]
                        (map (fn [[key value]]
                               (let [[xr yr wr hr xl yl v?]
                                     (case key
                                       :north [x (- y 50) w 30
                                               (- xm 20) (- y 30) false]
                                       :south [x (+ ye 19) w 30
                                               (- xm 20) (+ ye 40) false]
                                       :east [(+ xe 19) y 30 h
                                              (+ xe 40) (+ ym 20) true]
                                       :west [(- x 50) y 30 h
                                              (- x 30) (+ ym 20) true]
                                       nil)]
                                 {:xr xr :yr yr :width wr :height hr
                                  :xl xl :yl yl :v? v? :text value}))
                             (get-in c [:tf-config :wall-names])))
               :nodes [;;wall rect
                       {:tag    :rect :class :w-rect
                        :attr   {:x    :xr :y :yr :width :width :height :height
                                 :fill "darkgrey" :rx "6px" :ry "6px"}
                        :multi? true
                        :on     {:click (fn [this]
                                          (js/console.log "wall selected")
                                          #_(-> this
                                              (i/ocall :attr "stroke" "red")
                                              (i/ocall :attr "stroke-width" "2px")))}}
                       ;;wall-labels
                       {:tag    :text, :class :w-label
                        :attr   (merge (label-attr :xl :yl :v? :a)
                                       {:font-size "18px"})
                        :text   :text
                        :multi? true}]}
              ;; chamber group
              {:tag   :g, :class :chamber
               :data  (fn [c] (let [{tc    :tube-row-count, bc :burner-row-count
                                     :keys [tube-rows burner-rows]} (:tf-config c)
                                    sc (+ tc bc 1)
                                    sp (/ w sc)
                                    bf? (> bc tc)
                                    xb (if (= measure-level "middle")
                                         []
                                         (map #(+ (- x 15) sp (if bf? 0 sp) (* 2 sp %)) (range bc)))
                                    xt (map #(+ (- x 15) sp (if bf? sp 0) (* 2 sp %)) (range tc))]
                                {:xt-pos  xt, :xb-pos xb
                                 :tubes   tube-rows
                                 :burners burner-rows}))
               :nodes [;; tube row
                       {:tag    :rect, :class :t-row
                        :attr   {:x     identity, :y y1,
                                 :width 30, :height (* y1 2)
                                 :fill  "skyblue" :rx "6px" :ry "6px"}
                        :multi? true
                        :data   :xt-pos
                        :on     {:click (fn []
                                          (js/console.log "tube selected"))}}

                       ;; temp row
                       {:tag    :rect, :class :b-row
                        :attr   {:x     identity, :y y1,
                                 :width 30, :height (* y1 2)
                                 :fill  "lightgrey" :rx "6px" :ry "6px"}
                        :multi? true
                        :data   :xb-pos
                        :on     {:click (fn []
                                          (js/console.log "temp selected"))}}
                       ;; tube label
                       {:tag    :text, :class :t-label
                        :attr   (label-attr #(+ (:x %) 20) ym true "middle")
                        :text   :text
                        :multi? true
                        :data   (fn [{:keys [xt-pos tubes]}]
                                  (map (fn [x {name :name}]
                                         {:x x :text (str "Tube " name)})
                                       xt-pos tubes))}
                       ;; temp label
                       {:tag    :text, :class :b-label
                        :attr   (label-attr #(+ (:x %) 20) ym true "middle")
                        :text   :text
                        :multi? true
                        :data   (fn [{:keys [xb-pos burners]}]
                                  (map (fn [x i]
                                         {:x x :text (str
                                                       (case measure-level
                                                         "top" "Ceiling "
                                                         "bottom" "Floor ")
                                                       (inc i))})
                                       xb-pos (range (count burners))))}]}]}]))

(def ref-dwg
  {:view-box "0 0 600 500"
   :style    {:color          "grey"
              :fill           "none"
              :stroke         "grey"
              :stroke-width   "1px"
              :font-size      "14px"
              :font-family    "open_sans"
              :vertical-align "top"}
   :node     {:tag   :g, :class :reformer
              :nodes [{:tag  :rect, :class :back
                       :attr {:x     0, :y 0
                              :width 600, :height 500}}
                      {:tag   :g, :class :top-fired
                       :skip? #(not= "top" (:firing %))
                       :nodes top-fired-nodes}]}})

(defn tf-reformer-interactive
  [{:keys [width height preserve-aspect-ratio]}]
  [d3-svg (merge ref-dwg
                 {:width width, :height height
                  :preserve-aspect-ratio preserve-aspect-ratio
                  :data @(rf/subscribe [::subs/config])})])