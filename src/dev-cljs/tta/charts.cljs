(ns tta.charts
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.charts :as ht-charts]
            [ht.style :as ht-style]))

(defn gen-chart [n]
  (let [xi 0, xe 600
        yi 0, ye 400
        n (or n 100)
        x (map #(* % (/ xe n)) (random-sample 0.5 (range n)))
        f (fn [x xi yi] (+ (* x (/ yi xi)) (rand-int 50)))]
    (mapv (fn [x]
            {:x x
             :y (f x xe ye)})
          x)))

(defn charts []
  [:div {:style {:padding "20px"
                 :background "white"}} 
    [ht-charts/d3-chart {:data (gen-chart 100)}]])