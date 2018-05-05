(ns tta.charts
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.charts :as ht-charts]
            [ht.style :as ht-style]))

(def state (r/atom {:height 300, :width 700}))

(defn- any-temp [from to]
  (let [diff (- to from)]
    (+ from (rand-int diff))))

(defn twt-chart []
  (let [temp (partial any-temp 860 930)
        data (merge @state
                    {:burner-on? [true false true false false true]
                     :avg-temp-band [880 920]
                     :avg-raw-temp 910, :avg-temp 900
                     :max-temp 930, :min-temp 860
                     :start-tube 1, :end-tube 20
                     :design-temp 940, :target-temp 933
                     :side-names ["A" "B"]
                     :row-name "Tube Row 3"
                     :temps
                     [[(temp) (temp) nil nil (temp)
                       929 nil (temp) (temp) (temp)]
                      [(temp) nil (temp) (temp) nil
                       (temp) nil (temp) (temp) (temp)]]})]
    [ht-charts/twt-chart data]))

(defn overall-twt-chart []
  (let [max-temp 899, min-temp 701
        temp #(let [t (any-temp max-temp min-temp)]
                (if (< 720 t 880) t))
        trn 5, tn 20, brn 6, bn 10
        props (merge @state
                     {:wall-names {:north "North", :east "East"
                                   :south "South", :west "West"}
                      :tube-rows (mapv (fn [i]
                                         {:name (str "Tube row " (inc i))
                                          :start-tube 1, :end-tube tn})
                                       (range trn))
                      :burner-rows (mapv (fn [i]
                                           {:start-burner bn, :end-burner 1})
                                         (range brn))
                      :max-temp max-temp, :min-temp min-temp
                      :title "Overall tube wall temperature, Middle"
                      :sub-title "2018-10-14 | 14:50"
                      :burner-on? (-> (vec (repeat brn (vec (repeat bn true))))
                                      (assoc-in [0 3] false)
                                      (assoc-in [1 2] false)
                                      (assoc-in [1 3] false)
                                      (assoc-in [5 4] false)
                                      (assoc-in [5 5] false))
                      :temps (repeatedly
                              trn
                              (fn [] (repeatedly
                                     2
                                     (fn [] (repeatedly tn temp)))))})]
    [ht-charts/overall-twt-chart props]))

(defn charts []
  [:div
   ;; [twt-chart]
   [overall-twt-chart]
   ])
