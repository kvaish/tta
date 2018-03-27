(ns tta.app.charts
  (:require [clojure.set :as set]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [stylefy.core :as stylefy]
            [ht.app.subs :refer [translate]]
            [ht.style :as ht-style]
            [tta.app.icon :as ic]
            [tta.app.d3 :refer [d3-svg]]
            [tta.app.comp :as app-comp]))

(defn chart-layout [{:keys [
  height width
]}]
{:width width, :height height
:view-box (str "0 0 " width " " height)
:style {:color "white"
        :font-size "32px"}
:node {:tag :g
        :attr {:fill "none", :stroke "none"}
        :class :root  
        :nodes [
          ;; plot background
          {:tag :rect, :class :plot-bg
            :on {:mousedown #(js/console.log "mousedown-down:" [js/d3.event.offsetX
                          js/d3.event.offsetY])
                  :mouseup #(js/console.log "mouseup-at:" [js/d3.event.offsetX
                          js/d3.event.offsetY])}
            :attr {:x 0, :y 0, :width width, :height height, :fill "aliceblue"}
          }

          ;; plot
          {:tag :circle, :class :point
            :attr {:r 3
                    :cx :x, :cy :y
                    :fill "red"}
            :multi? true                   
            :data :points
            :on {:click #(js/console.log "click:" [js/d3.event.pageX
                            js/d3.event.pageY])}}]}})

(defn d3-chart [{:keys [data config]}]
  (let [state (r/atom {
          :data data
          :visible-area {}
  })]
    [d3-svg (assoc (chart-layout {
      :width 600
      :height 400
    })
                        :data data)]))