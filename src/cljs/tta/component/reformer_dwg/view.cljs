;; view elements component reformer-dwg
(ns tta.component.reformer-dwg.view
  (:require [cljs.core.async :refer [<! put!]]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.util.common :as u :refer [dev-log]]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.app.d3 :refer [d3-svg get-value d3-svg-2-string]]
            [tta.component.reformer-dwg.style :as style]
            [tta.component.reformer-dwg.subs :as subs]
            [tta.component.reformer-dwg.event :as event])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn label-attr
  ([x y] (label-attr x y false nil))
  ([x y v?] (label-attr x y v? nil))
  ([x y v? a]
   {:stroke "none", :fill "black"
    :x x, :y y
    :transform #(if (get-value v? %1 %2 %3)
                  (let [x (get-value x %1 %2 %3)
                        y (get-value y %1 %2 %3)]
                    (str "rotate(270," x "," y ")")))
    :text-anchor a}))

(def side-fired-nodes
  [;; whs group
   {:tag :g, :class :whs
    :skip? #(empty? %)
    :data #(case (get-in % [:sf-config :placement-of-WHS])
             "end" {:w 240, :h 50, :x 180, :y 50
                    :tx 250, :ty 90}
             "side" {:w 60, :h 200, :x 480, :y 170
                     :tx 525, :ty 320, :tt "rotate(270,525,320)"}
             ;; return an empty map in other cases to skip drawing it
             {})
    :nodes [;; whs box
            {:tag :rect, :class :whs-box
             :attr {:x :x, :y :y, :width :w, :height :h}}
            ;; whs label
            {:tag :text, :class :whs-label
             :text "WHS"
             :attr {:x :tx, :y :ty, :transform :tt
                     :font-size "45px"
                     :stroke "none", :fill "black"}}]}
   ;; chamber group
   {:tag :g, :class :chamber
    :multi? true
    :data (fn [config _ _]
            (let [chs (get-in config [:sf-config :chambers])
                  a (case (count chs)
                      1 {:w 360, :h 350, :x 120, :y 100, :xm 300, :ym 275}
                      2 {:w 180, :h 350, :x 60, :y 100, :xm 150, :ym 275})]
              (map #(-> (assoc a :ch %)
                        (update :x + (* 300 %2))
                        (update :xm + (* 300 %2)))
                   chs (range))))
    :nodes [ ;; chamber box
            {:tag :rect, :class :ch-box
             :attr {:height :h, :width, :w, :y :y, :x :x,
                    :stroke "black" :stroke-width "5px"}}
            ;; section label
            {:tag :text, :class :s-label
             :attr (label-attr #(- (:x %) 6) 106 true "end")
             :text #(str (get-in % [:ch :section-count]) " Sections")}
            ;; chamber label
            {:tag :text, :class :ch-label
             :attr (label-attr :xm 465 nil "middle")
             :text [:ch :name]}
            ;; tube row
            {:tag :line, :class :t-row
             :attr {:stroke-width "10px"
                     :x1 :xm, :y1 150
                     :x2 :xm, :y2 400
                     :stroke-linecap "round"}}
            ;; tube label
            {:tag :text, :class :t-label
             :attr (label-attr #(- (:xm %) 10) :ym true "middle")
             :text #(let [{st :start-tube, et :end-tube} (:ch %)
                          sep (if (> et st) " ← " " → ")]
                      (str "Tubes  " et sep st))}
            ;; side A label
            {:tag :text, :class :sa-label
             :attr (label-attr #(- (:x %) 6) 444 true)
             :text [:ch :side-names 0]}
            ;; side B label
            {:tag :text, :class :sb-label
             :attr (label-attr #(+ (:x %) (:w %) 15) 444 true)
             :text [:ch :side-names 1]}
            ;; side A burner label
            {:tag :text, :class :ba-label
             :attr (label-attr #(+ (:x %) 15) :ym true "middle")
             :text #(let [{sb :start-burner, eb :end-burner} (:ch %)
                          sep (if (> eb sb) " ← " " → ")]
                      (str "Burners  " eb sep sb))}
            ;; side B burner label
            {:tag :text, :class :bb-label
             :attr (label-attr #(- (+ (:x %) (:w %)) 6) :ym true "middle")
             :text #(let [{sb :start-burner, eb :end-burner} (:ch %)
                          sep (if (> eb sb) " ← " " → ")]
                      (str "Burners  " eb sep sb))}]}])

(def top-fired-nodes
  (let [w 600, h 500, m 50, p 20
        x m, y m, w (- w m m), h (- h m m)
        xe (+ x w), ye (+ y h)
        xm (+ x (/ w 2)), ym (+ y (/ h 2))
        y1 (+ y p), y2 (- ye p)]
    [{:tag :g, :class :reformer
      :nodes [;; chamber box
              {:tag :rect, :class :ch-box
               :attr {:x x, :y y, :width w, :height h
                      :stroke "black", :stroke-width "5px"}}
              ;; section label
              #_{:tag :text, :class :s-label
               :attr (label-attr (- x 6) (+ y 6) true "end")
               :text #(str (get-in % [:tf-config :section-count]) " Sections")}
              ;; wall label
              {:tag :text, :class :w-label
               :attr (merge (label-attr :x :y :v? :a)
                             {:font-size "18px"})
               :text :text
               :multi? true
               :data (fn [c] (map (fn [[key value]]
                                   (let [[x y v?] (case key
                                                    :north [xm (- y 6) false]
                                                    :south [xm (+ ye 19) false]
                                                    :east [(+ xe 19) ym true]
                                                    :west [(- x 6) ym true]
                                                    nil)]
                                     {:text value
                                      :x x, :y y, :v? v?, :a "middle"}))
                                 (get-in c [:tf-config :wall-names])) )}
              ;; chamber group
              {:tag :g, :class :chamber
               :data (fn [c] (let [{tc :tube-row-count, bc :burner-row-count
                                   :keys [tube-rows burner-rows]} (:tf-config c)
                                  sc (+ tc bc 1)
                                  sp (/ w sc)
                                  bf? (> bc tc)
                                  xb (map #(+ x sp (if bf? 0 sp) (* 2 sp %)) (range bc))
                                  xt (map #(+ x sp (if bf? sp 0) (* 2 sp %)) (range tc))]
                              {:xt-pos xt, :xb-pos xb
                               :tubes tube-rows
                               :burners burner-rows}))
               :nodes [;; row labels
                       {:tag :text, :class :r-label
                        :attr (merge
                               (label-attr #(- (:x %) 6) y1 true "end")
                               {:font-size "16px"
                                :stroke "black"})
                        :text :text
                        :multi? true
                        :data (fn [{:keys [xt-pos tubes]}]
                                (map (fn [x {name :name}]
                                       {:x x :text name})
                                       xt-pos tubes))}
                       ;; tube row
                       {:tag :line, :class :t-row
                        :attr {:x1 identity, :y1 y1, :x2 identity, :y2 y2
                                :stroke-width "5px"
                                :stroke-linecap "round"}
                        :multi? true
                        :data :xt-pos}
                       ;; tube label
                       {:tag :text, :class :t-label
                        :attr (label-attr #(- (:x %) 6) ym true "middle")
                        :text :text
                        :multi? true
                        :data (fn [{:keys [xt-pos tubes]}]
                                (map (fn [x {st :start-tube, et :end-tube}]
                                       (let [sep (if (> et st) " ← " " → ")]
                                         {:x x :text (str "Tubes " et sep st)}))
                                     xt-pos tubes ))}
                       ;; burner row
                       {:tag :line, :class :b-row
                        :attr {:x1 identity, :y1 y1, :x2 identity, :y2 y2
                                :stroke-width "1px"
                                :stroke "red"
                                :stroke-dasharray "10 5 2 5 2 5"
                                :stroke-linecap "round"}
                        :multi? true
                        :data :xb-pos}
                       ;; burner label
                       {:tag :text, :class :b-label
                        :attr (label-attr #(- (:x %) 6) ym true "middle")
                        :text :text
                        :multi? true
                        :data (fn [{:keys [xb-pos burners]}]
                                (map (fn [x {sb :start-burner, eb :end-burner}]
                                       (let [sep (if (> eb sb) " ← " " → ")]
                                         {:x x :text (str "Burners " eb sep sb)}))
                                     xb-pos burners))}]}]}]))

(def ref-dwg
  {:view-box "0 0 600 500"
   :style {:color "grey"
           :fill "none"
           :stroke "grey"
           :stroke-width "1px"
           :font-size "14px"
           :font-family "open_sans"
           :vertical-align "top"}
   :node {:tag :g, :class :reformer
          :nodes [{:tag :rect, :class :back
                   :attr {:x 0, :y 0
                          :width 600, :height 500
                          :fill "lightgrey"}}
                  {:tag :g, :class :side-fired
                   :skip? #(not= "side" (:firing %))
                   :nodes side-fired-nodes}
                  {:tag :g, :class :top-fired
                   :skip? #(not= "top" (:firing %))
                   :nodes top-fired-nodes}]}})

(defn reformer-dwg [{:keys [width height preserve-aspect-ratio config]}]
  [d3-svg (merge ref-dwg
                 {:width width, :height height
                  :preserve-aspect-ratio preserve-aspect-ratio
                  :data (or config
                            @(rf/subscribe [::subs/config]))})])

(defn save-image [{:keys [config]}]
  (let [width 600, height 500
        svg-string (d3-svg-2-string
                    (merge ref-dwg
                           {:width width, :height height
                            :preserve-aspect-ratio "none"
                            :data (or config
                                      @(rf/subscribe [::subs/config]))}))]
    (go
      (let [res (<! (u/save-svg-to-file "reformer-drawing.png"
                                        svg-string width height 10))]
        (dev-log "download reformer drawing: " (name res))))))
