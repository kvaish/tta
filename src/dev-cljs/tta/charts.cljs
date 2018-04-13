(ns tta.charts
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.charts :as ht-charts]
            [ht.style :as ht-style]))

(defn charts []
  (let [state (r/atom {}) 
        config {:height 400, :width 600 
                :red-firing [[2 3] [7 7] [10 13]], 
                :avg-temp-band [850 880] 
                :avg-raw-temp 815, :avg-corr-temp 825
                :title "Tube row no. 10" 
                :x-title "Tube no" :x-domain [0 16]
                :y-title "Corrected TWT (deg C)" :y-domain [800 910]
                :design-temp 885 :target-temp 280}
        data [{:tube-no 2 :temperatures [840 820]}
              {:tube-no 4 :temperatures [820 860]}
              {:tube-no 8 :temperatures [840 830]}
              {:tube-no 10 :temperatures [900 885]}
              {:tube-no 12 :temperatures [870 865]}
              {:tube-no 15 :temperatures [875 745]}]]
    [ht-charts/twt-chart config data]))
