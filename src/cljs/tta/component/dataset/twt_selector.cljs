(ns tta.component.dataset.twt-selector
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.util.interop :as i]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.d3 :refer [d3-svg get-value]]))

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

(defn click-handler [_ {:keys [level-key selected? select on-select]} _ _]
  (if (and select on-select (not selected?))
    (on-select (assoc select :level-key level-key))))

(def selected-stroke
  {:stroke (fn [{:keys [selected?]} _ _]
             (if selected? "#f55197")) ;; monet-pink
   :stroke-width (fn [{:keys [selected?]} _ _]
                   (if selected? 2 1))
   :stroke-dasharray (fn [{:keys [selected?]} _ _]
                       (if selected? "15,5"))})

(def dwg-node
  (let [bw 600, bh 400, m 45, p 10
        x m, y m, w (- bw m m), h (- bh m m)
        xe (+ x w), ye (+ y h)
        xm (+ x (/ w 2)), ym (+ y (/ h 2))]
    {:tag :g, :class :reformer
     :nodes [;; background
             #_{:tag :rect, :class :back
              :attr {:fill "#d0d3d4" ;; alumina-grey
                     :x 0, :y 0, :width bw, :height bh, :rx "6px", :ry "6px"}}
             ;; chamber box
             {:tag :rect, :class :ch-box
              :attr {:x x, :y y, :rx "6px", :ry " 6px"
                     :width w, :height h
                     :stroke "#323c46", :stroke-width "3px"}}

             ;; wall selectors
             {:tag :g, :class :wall-selector
              :data (fn [data]
                      (map (fn [[key value]]
                             (let [a 30, b (+ a p)
                                   [x y w h xl yl v?]
                                   (case key
                                     :north [x (- y b) w a
                                             xm (- y p 10) false]
                                     :south [x (+ ye p) w a
                                             xm (+ ye b -10) false]
                                     :east [(+ xe p) y a h
                                            (+ xe b -10) ym true]
                                     :west [(- x b) y a h
                                            (- x p 10) ym true]
                                     nil)
                                   select {:scope :wall, :index key}]
                               (assoc data :x x, :y y, :w w, :h h
                                      :xl xl, :yl yl, :v? v?, :text value
                                      :select select
                                      :selected? (= select (:selected data)))))
                           (:wall-names data)))
              :attr {:cursor "pointer"}
              :multi? true
              :on {:click click-handler}
              :nodes [ ;;wall rect
                      {:tag :rect, :class :w-rect
                       :attr (merge {:x :x, :y :y, :width :w, :height :h
                                     :fill "#7c868d" ;; slate-grey
                                     :rx "6px", :ry "6px"}
                                    selected-stroke)}
                      ;;wall label
                      {:tag :text, :class :w-label
                       :attr (label-attr :xl :yl :v? "middle")
                       :text :text}]}

             ;; tube selectors
             {:tag :g, :class :tube-selector
              :data (fn [data]
                      (let [row-names (:tube-row-names data)
                            n (count row-names)
                            n2 (inc (* 2 n))
                            w (/ (- w (* (inc n2) p)) n2)]
                        (map-indexed (fn [i row-name]
                                       (let [x (+ x
                                                  (* w (inc (* 2 i)))
                                                  (* p (+ 2 (* 2 i))))
                                             y (+ y p)
                                             select {:scope :tube, :index i}]
                                         (assoc data
                                                :x x, :y y
                                                :w w, :h (- h p p)
                                                :xl (+ x (/ w 2) 4), :yl ym
                                                :text row-name
                                                :select select
                                                :selected? (= select (:selected data)))))
                                     row-names)))
              :attr {:cursor "pointer"}
              :multi? true
              :on {:click click-handler}
              :nodes [;; tube rect
                      {:tag :rect, :class :t-rect
                       :attr (merge {:x :x, :y :y, :width :w, :height :h
                                     :fill "#54c9e9" ;; sky-blue
                                     :rx "6px", :ry "6px"}
                                    selected-stroke)}
                      ;; tube label
                      {:tag :text, :class :t-label
                       :attr (label-attr :xl :yl true "middle")
                       :text :text}]}

             ;; ceiling/floor selectors
             {:tag :g, :class :tube-side-row-selector
              :data (fn [{:keys [tube-row-names level-key selected
                                tube-side-row-key tube-side-row-label]
                         :as data}]
                      (let [row-names tube-row-names
                            n (count row-names)
                            n2 (inc (* 2 n))
                            w (/ (- w (* (inc n2) p)) n2)]
                        (map (fn [i]
                               (let [x (+ x
                                          (* w (* 2 i))
                                          (* p (inc (* 2 i))))
                                     y (+ y p)
                                     select (if (not= level-key :middle)
                                              {:scope tube-side-row-key
                                               :index i})]
                                 (assoc data
                                        :x x, :y y
                                        :w w, :h (- h p p)
                                        :xl (+ x (/ w 2) 4), :yl ym
                                        :text (if select
                                                (str tube-side-row-label
                                                     " " (inc i)))
                                        :select select
                                        :selected? (if select (= select selected)))))
                             (range (inc n)))))
              :attr {:cursor (fn [{:keys [select]}] (if select "pointer"))}
              :multi? true
              :on {:click click-handler}
              :nodes [;; tube side row rect
                      {:tag :rect, :class :tsr-rect
                       :attr (merge {:x :x, :y :y, :width :w, :height :h
                                     :fill "#d0d3d4" ;; alumina-grey
                                     :rx "6px", :ry "6px"}
                                    selected-stroke)}
                      ;; tube side row label
                      {:tag :text, :class :tsr-label
                       :attr (label-attr :xl :yl true "middle")
                       :text :text}]}]}))

(defn twt-selector
  "**selected** {:scope <:wall|:tube|:ceiling|:floor>
                 :index <wall-key | row-index>}
   **on-select** (fn [{:keys[scope index level-key]}])"
  [{:keys [width height preserve-aspect-ratio
           wall-names tube-row-names
           tube-side-row-key tube-side-row-label
           level-key
           selected on-select]}]
  [d3-svg
   {:view-box "0 0 600 400"
    :width width, :height height
    :preserve-aspect-ratio preserve-aspect-ratio
    :style    {:user-select    "none"
               :color          "grey"
               :fill           "none"
               ;; :stroke         "grey"
               ;; :stroke-width   "1px"
               :font-size      "10px"
               :font-family    "open_sans"
               :vertical-align "top"}
    :data {:wall-names wall-names
           :tube-row-names tube-row-names
           :tube-side-row-key tube-side-row-key
           :tube-side-row-label tube-side-row-label
           :level-key level-key
           :selected selected
           :on-select on-select}
    :node dwg-node}])
