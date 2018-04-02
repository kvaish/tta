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

(defn abs [n] (max n (- n)))

(defn chart-layout [{
  :keys [height width bgevents]
}]
{:width width, :height height
:view-box (str "0 0 " width " " height)
:style {:color "white"
        :font-size "32px"}
:node {:tag :g
        :attr {:fill "none", :stroke "none"}
            
        :class :root
        :on bgevents
        :nodes [

          ;; plot background
          {:tag :rect, :class :bg
            :attr {:x 0, :y 0, :width width, :height height, :fill "aliceblue"}
          }

          ;; zoom area
          {:tag :rect, :class :zoom
            :attr {:x :x, :y :y, :width :width, :height :height, :fill "pink"}
            :data :zoom
          }

          ;; plot
          {:tag :g
            :attr {:fill "none", :stroke "none" }
            :class :series
            :nodes [
              {:tag :circle, :class :point
                :attr {:r 3 :cx :x, :cy :y 
                        :fill "red"}
                :multi? true
                :data :points}
            ]}
          ]}})

(defn d3-chart [{:keys [data config zoom-fn]}]
  (let [state (r/atom {:bg {}})

        height 400 width 600

        ;; returns a new set of [x y] to plot
        zoom (or zoom-fn 
          (fn [ series {xi :x yi :y h :height w :width 
                       :or {xi 0 yi 0 h height w width}}]
            (let [hf (/ height h) wf (/ width w)]
              (mapv (fn [{x :x y :y}] { :x (- (* x wf) (* xi wf)) 
                :y (- (* y hf) (* yi hf))}) series))))

        pan (fn [ series {:keys [left top]}]
          (mapv (fn [{x :x y :y}] { :x (- x left) 
                :y (- y top)}) series))

        bg-mousedown (fn [ev]
          (swap! state update-in [:bg :selected] assoc 
            :x js/d3.event.offsetX
            :y js/d3.event.offsetY
            :x0 js/d3.event.offsetX
            :y0 js/d3.event.offsetY)
          (if (get-in @state [:modes :pan]) (swap! state assoc :panning? true)))

        bg-mousemove (fn [ev] 
          (if (get-in @state [:modes :zoom]) (swap! state update-in [:bg :selected] (fn [selected]
            (if selected (let [
                cx js/d3.event.offsetX cy js/d3.event.offsetY
                w (abs (- cx (:x0 selected))) h (abs (- cy (:y0 selected)))
                x (min (:x0 selected) cx) y (min (:y0 selected) cy)]
              (assoc selected :width w :height h :x x :y y))))))

          (if (:panning? @state) (do 
            (swap! state update-in [:bg :zoom-area] (fn [zoom-area]
              (if zoom-area (let [
                  cx js/d3.event.offsetX cy js/d3.event.offsetY
                  {sx :x sy :y} (get-in @state [:bg :selected])
                  w (- cx sx) h (- cy sy)
                  lx (or (:x0 zoom-area) (:x zoom-area))
                  ly (or (:y0 zoom-area) (:y zoom-area))
                  x (- lx w) y (- ly h)]
                (assoc zoom-area :x x :y y :x0 lx :y0 ly)
                    )))
                  
              (swap! state update-in [:prev-data :points] 
                zoom (get-in @state [:bg :zoom-area])))
            (js/console.log (get-in @state [:bg :zoom-area])))))

        bg-mouseup (fn [ev]
          (swap! state update :bg (fn [{:keys [selected zoom-area]}]
            (let [{h :height w :width} selected
                  bg {:selected nil}]
              (if (and (> h 0) (> w 0) (get-in @state [:modes :zoom])) (assoc bg :zoom-area selected) bg))))
          (if (get-in @state [:modes :zoom]) (swap! state update-in [:plot :points] 
            zoom (get-in @state [:bg :zoom-area])))
          (swap! state assoc :panning? false)
          )

        bg-mouseleave (fn [ev]
          (let [x js/d3.event.offsetX
                y js/d3.event.offsetY]
            (if (not (and (<= 0 x width) (<= 0 y height))) 
              (swap! state assoc-in [:bg :selected] nil))
            (swap! state assoc :panning? false)))

        layout (chart-layout {
                  :width width :height height
                  :bgevents { :mousedown bg-mousedown
                              :mousemove bg-mousemove
                              :mouseup bg-mouseup
                              :mouseleave bg-mouseleave}})]
    (r/create-class
      { :component-will-mount 
          (fn [this] nil)

        :reagent-render
        (fn [{:keys [data config zoom-fn]}]
          (if (not= (:prev-data @state) data) 
            (do (swap! state assoc :prev-data data)
                (swap! state assoc-in [:plot :points] 
                  (zoom (:points data) (get-in @state [:bg :zoom-area])))))
          (let []
            [:div {:style {:display "block"}}
              [:div 
                [:button {:style {:display "inline-block"}
                        :on-click #(do (swap! state assoc-in [:bg :zoom-area] 
                                      {:x 0 :y 0 :height height :width width})
                                      (swap! state assoc :plot data))} 
                  "Reset"]
                [:button {:style {:display "inline-block" :background-color (if (get-in @state [:modes :zoom]) "grey")}
                          :on-click #(swap! state update-in [:modes :zoom] not)}
                  "Toggle zoom"]
                [:button {:style {:display "inline-block" :background-color (if (get-in @state [:modes :pan]) "grey")}
                          :on-click #(swap! state update-in [:modes :pan] not)}
                  "Toggle pan"]]
              [d3-svg (assoc layout 
                :data (assoc (:plot @state)
                :zoom (get-in @state [:bg :selected])))]]))})))