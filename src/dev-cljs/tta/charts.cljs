(ns tta.charts
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.charts :as ht-charts]
            [ht.style :as ht-style]))

(defn- any-temp [from to]
  (let [diff (- to from)]
    (+ from (rand-int diff))))

(defn temp->color [temp]
  (cond 
    (and (<= 860 temp) (< temp 880)) "teal"
    (and (<= 880 temp) (< temp 900)) "orange"
    (and (<= 900 temp) (< temp 920)) "green"
    (and (<= 920 temp) (< temp 940)) "blue"))

(defn charts []
  (let [state (r/atom {})
        temp (partial any-temp 860 930) 
        config_twt 
          { :height 350, :width 700 
            :red-firing [[2 3] [7 9] [12 13]], 
            :avg-temp-band [890 910] 
            :avg-raw-temp 865, :avg-corr-temp 875
            :x-title "Tube Row 01" :x-domain [1 17]
            :y-title "Corrected TWT (deg C)" :y-domain [860 940]
            :design-temp 885, :target-temp 870, 
            :side-names {:a "Left" :b "Right"}}

        data_twt 
          [ 
            {:tube 2 :a (temp) :b (temp)}
            {:tube 5 :a (temp) :b (temp)}
            {:tube 6 :a (temp) :b (temp)}
            {:tube 8 :a (temp) :b (temp)}
            {:tube 12 :a (temp) :b (temp)}
            {:tube 15 :a (temp) :b (temp)}
            {:tube 16 :a (temp) :b (temp)}]

        config_twt_overall
          { :y-title "Tube number"
            :height 350, :width 700
            :temp->color temp->color
            :wall-names {:north "north" :east "east" :west "west" :south "south"}
            :burner-first? false
            :y-domain [1 12]}
        data_twt_overall {
            :tube-data 
              [{:row-no 1
                :name "Tube row 1"
                :red-firing {:a [3 5] :b [6 8]}
                :tube-count 12
                :start-tube 1
                :end-tube 12
                :temperatures {:a [(temp) (temp) (temp) (temp)] :b [(temp) (temp) (temp) (temp)]}}
               {:row-no 2
                :name "Tube row 2"
                :red-firing {:a [3 5] :b [6 8]}
                :tube-count 12
                :start-tube 1
                :end-tube 12
                :temperatures {:a [(temp) (temp)] :b [(temp) (temp)]}}
               {:row-no 3
                :name "Tube row 3"
                :red-firing {:a [3 5] :b [6 8]}
                :tube-count 12
                :start-tube 1
                :end-tube 12
                :temperatures {:a [(temp) (temp)] :b [(temp) (temp)]}}
               {:row-no 4
                :name "Tube row 4"
                :red-firing {:a [3 5] :b [6 8]}
                :tube-count 12
                :start-tube 1
                :end-tube 12
                :temperatures {:a [(temp) (temp)] :b [(temp) (temp)]}}]
            :burner-data 
              [{ :burner-count 5 :start-burner 1 :end-burner 5}
              { :burner-count 5 :start-burner 1 :end-burner 5}
              { :burner-count 5 :start-burner 1 :end-burner 5}]}]
    [:div
      [ht-charts/overall-twt-chart config_twt_overall data_twt_overall]
      [ht-charts/twt-chart config_twt data_twt]]))