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

(defn temp->color [temp]
  (cond
    (and (<= 860 temp) (< temp 880)) "teal"
    (and (<= 880 temp) (< temp 900)) "orange"
    (and (<= 900 temp) (< temp 920)) "deepskyblue"
    (and (<= 920 temp) (< temp 940)) "lime"))

(defn twt-chart []
  (let [temp (partial any-temp 860 930)
        data (merge @state
                    {;:reduced-firing-bands [[2 3] [7 9]]
                     :burner-on? [true false true false false true]
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

(defn overall-chart []
  (let [temp (partial any-temp 860 930)
        tubes-per-row 20
        config_twt_overall
          { :height 350, :width 700
            :temp->color temp->color
            :wall-names {:north "North wall" :east "East wall"
                         :west "West wall" :south "South wall"}
            :burner-first? false
            :y-domain [1 tubes-per-row]}
        data_twt_overall {
            :tube-data 
              [{:row-no 1
                :name "Tube row 1"
                :red-firing {:a [3 5] :b [6 8]}
                :tube-count tubes-per-row
                :start-tube 1
                :end-tube tubes-per-row
                :temperatures {:a (repeatedly tubes-per-row temp) 
                               :b (repeatedly tubes-per-row temp)}}
               {:row-no 2
                :name "Tube row 2"
                :red-firing {:a [3 5] :b [6 8]}
                :tube-count tubes-per-row
                :start-tube 1
                :end-tube tubes-per-row
                :temperatures {:a (repeatedly tubes-per-row temp) 
                               :b (repeatedly tubes-per-row temp)}}
               {:row-no 3
                :name "Tube row 3"
                :red-firing {:a [3 5] :b [6 8]}
                :tube-count tubes-per-row
                :start-tube 1
                :end-tube tubes-per-row
                :temperatures {:a (repeatedly tubes-per-row temp) 
                               :b (repeatedly tubes-per-row temp)}}
               {:row-no 4
                :name "Tube row 4"
                :red-firing {:a [3 5] :b [6 8]}
                :tube-count tubes-per-row
                :start-tube 1
                :end-tube tubes-per-row
                :temperatures {:a (repeatedly tubes-per-row temp) 
                               :b (repeatedly tubes-per-row temp)}}]
            :burner-data 
              [{ :burner-count 5 :start-burner 1 :end-burner 5}
              { :burner-count 5 :start-burner 1 :end-burner 5}
              { :burner-count 5 :start-burner 1 :end-burner 5}]}]
    #_[ht-charts/overall-twt-chart config_twt_overall data_twt_overall]))

(defn charts []
  [:div
   [twt-chart]])
