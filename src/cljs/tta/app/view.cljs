(ns tta.app.view
  "collection of common small view elements for re-use"
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljsjs.material-ui]
            [cljs-react-material-ui.reagent :as ui]
            [ht.util.interop :as i]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.common :refer [trans-motion spring]]
            [tta.app.style :as style]
            [tta.app.subs :as subs]
            [tta.app.event :as event]
            [tta.app.style :as app-style]))


(defn layout-main [title sub-title actions body]
  (let [view-size @(rf/subscribe [::ht-subs/view-size])
        body-size (style/content-body-size view-size)
        style (style/layout-main view-size)]
    [:div (use-style style)
     [:div (use-sub-style style :head)
      [:div (use-sub-style style :head-left)
       [:span (use-sub-style style :title) title]
       [:span (use-sub-style style :sub-title) sub-title]]
      (into [:div (use-sub-style style :head-right)] actions)]
     [:div (use-sub-style style :body)
      (if body
        [body body-size])]]))

(defn vertical-line [{:keys [height]}]
  (let [style (style/vertical-line height)]
    [:div (use-style style)
     [:div (use-sub-style style :line)]]))

(defn- tab-head [{:keys [index label width last? position
                         on-select selected?]}]
  (let [width (if width (if last? width (dec width)))
        style (app-style/tab-head position selected? last?)]
    [:div (-> (use-style style)
              (update :style assoc :width width)
              (assoc :on-click (if-not selected? #(on-select index))))
     label]))

(defn- tab-bar
  "**position** is :top or :bottom"
  [{:keys [labels selected width position on-select]}]
  (into [:div {:style {:width width, :height 26
                       :position "absolute"
                       :left 0, position 0}}]
        (let [lc (count labels)
              w (if width (/ width lc))]
          (map #(vector tab-head {:label %1, :index %2
                                  :width w, :key %2
                                  :last? (= (inc %2) lc)
                                  :on-select on-select
                                  :selected? (= selected %2)
                                  :position position})
               labels (range)))))

(defn tab-layout
  "**top-tabs**: {:selected _, on-select _, :labels _}  
  **bottom-tabs**: {:selected _, on-select _, :labels _}  
  **labels**: [label ...]  
  **on-select**: (fn [index])"
  [props]
  (let [state (atom {})
        will-leave (fn [s]
                     (let [k (i/oget s :key)
                           {:keys [dir style]} @state]
                       (or (get style k)
                           (let [[t l] (case dir
                                         :d [1 0], :u [-1 0]
                                         :r [0 1], :l [0 -1]
                                         [0 0])
                                 style #js{:t (spring t)
                                           :l (spring l)
                                           :o (spring 0)}]
                             (swap! state assoc-in [:style k] style)
                             style))))
        will-enter (fn [s]
                     (let [k (i/oget s :key)]
                       (swap! state assoc-in [:style k] nil))
                     (let [[t l] (case (:dir @state)
                                   :d [-1 0], :u [1 0]
                                   :r [0 -1], :l [0 1]
                                   [0 0])]
                       #js{:t t, :l l, :o 0}))]
    (fn [{:keys [top-tabs bottom-tabs width height content]}]
      (let [top-tabs (not-empty top-tabs)
            bot-tabs (not-empty bottom-tabs)
            top-sel (:selected top-tabs)
            bot-sel (:selected bot-tabs)
            current-key (str top-sel "," bot-sel)
            ;; compute layout
            [{:keys [h2 w2 t2 h3 w3]} style]
            (app-style/tab-layout (some? top-tabs) (some? bot-tabs) width height)]
        (swap! state (fn [s]
                       (let [pos [top-sel bot-sel]
                             old (:pos s)
                             [pt pb] pos
                             [ot ob] old
                             dir (cond
                                   (nil? old) :r
                                   (< pt ot) :r
                                   (> pt ot) :l
                                   (< pb ob) :d
                                   (> pb ob) :u)]
                         (assoc s :pos pos, :dir dir))))
        ;; main container(1)
        [:div (update (use-style style) :style assoc
                      :width width, :height height)
         ;; use motion for swiping out old content
         [trans-motion
          {:willLeave will-leave
           :willEnter will-enter
           :styles #js[#js{:key current-key
                           :data [top-sel bot-sel]
                           :style #js{:t (spring 0), :l (spring 0), :o (spring 1)}}]}
          (fn [interpolations]
            (r/as-element
             ;; container(2) for tab-bar and content-container
             [:div (update (use-sub-style style :div2) :style assoc
                           :width w2, :height h2, :top t2)
              [:div (update (use-sub-style style :div3) :style
                            assoc :width w3, :height h3)
               (->> interpolations
                    (map #(js->clj % :keywordize-keys true))
                    (map (fn [s]
                           (let [{k :key, [ts bs] :data, {:keys [t l o]} :style} s
                                 t (* t h3)
                                 l (* l w3)]
                             [(if (= k current-key) 0 1)
                              [:div (-> (use-sub-style style :div3)
                                        (assoc :key k)
                                        (update :style assoc
                                                :opacity (+ 0.2 (* 0.8 o))
                                                :width w3, :height h3
                                                :top t, :left l))
                               ;; the content
                               [content {:width w3, :height h3
                                         :selected [ts bs]}]]])))
                    (remove #(nil? (second %)))
                    (sort-by first <)
                    (map second)
                    (doall))]]))]
         (if top-tabs
           [tab-bar (assoc top-tabs :width width, :position :top)])
         (if bot-tabs
           [tab-bar (assoc bot-tabs :position :bottom)])]))))
