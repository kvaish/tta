(ns tta.app.charts
  (:require [clojure.set :as set]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [stylefy.core :as stylefy]
            [ht.app.subs :refer [translate]]
            [ht.style :as ht-style]
            [tta.app.icon :as ic]
            [ht.util.interop :as i]
            [cljsjs.d3]
            [tta.app.d3 :refer [d3-svg]]
            [tta.app.comp :as app-comp]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Terms:
;; x-ps-os: From where the plot starts.
;;              So the bands start here.
;; x-as-os: From where the x-axis starts relative to plot start
;; x-pe-os: Where the band ends
;; x-ae-os: Where the axis ends
;; Similar for the Y-axis (from bottom)
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


;; Helpers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn d3-scale [domain range]
  (let [scale (-> js.d3
                  (i/ocall :scaleLinear)
                  (i/ocall :domain (to-array domain))
                  (i/ocall :range (to-array range)))]
    scale))

(defn d3-ticks [scale]
  (vec (i/ocall scale :ticks)))

(defn- abs [n] (max n (- n)))

(defn- linear-scale [[x1 x2] [y1 y2] data]
  ;; scale data from domain to range
  (let [factor (/ (- y2 y1) (- x2 x1))]
    (if (seqable? data) (mapv #(+ y1 (* factor (- % x1))) data)
     (+ y1 (* factor (- data x1))))))

(defn- create-alt-bands [points start]
  (let [count (count points)
        y1 (:y (points 0))
        y2 (:y (points 1))
        height (abs (- y1 y2))]
    (mapv (fn [p] {:x start :y (- (:y p) height) :height height :fill "aliceblue"}) 
      (take-nth 2 points))))

(defn take-every [nth bounds]
  (let [[start end] (vec (sort bounds))]
    (take-nth nth (range start 
      (if (= 0 (mod (- end start) nth)) (inc end) end)))))

(defn format-to-n [num digits]
  (let [s (str num), c (count s)]
    (if (>= c digits) s
      (str (apply str (repeat (- digits c) 0)) num))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Layouts ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Horizontal Bands
(defn- ch-horizontal-bands [data-key width]
  { :tag :rect, :class :bg-horizontal-bands
    :attr {:x :x :y :y :height :height :width width :fill :fill}
    :data data-key
    :multi? true})

;; Vertical Bands
(defn- ch-vertical-bands [data-key height]
  { :tag :rect, :class :bg-vertical-bands
    :attr {:x :x :y 0 :height height :width :width :fill :fill}
    :data data-key
    :multi? true})

;; Points
(defn- ch-points [data-key radius fill events]
  {:tag :g :class :series
   :attr {:fill "none", :stroke "none"}
   :multi? true :data data-key
   :nodes [{:tag :circle, :class data-key
            :attr {:r radius :cx :x, :cy :y 
                    :fill fill}
            :multi? true
            :on events
            :data :data}]})

;; Data rectangles
(defn- ch-data-rects [data-key]
  {:tag :rect, :class :data-rectangles
   :data data-key :multi? true
   :attr {:x :x :y :y :height :height :width :width :stroke :fill :fill :fill}})

;; Horizontal lines
(defn- ch-horizontal-lines [data-key x width] 
  { :tag :g :class :hor-lines
    :attr {:fill "none", :stroke "none"}
    :multi? true :data data-key
    :nodes 
      [ ;; title
        { :tag :text, :class :line-title
          :text :text
          :attr { :x (+ 40 x), :y #(- (:y %) 5)
                  :style "font-size: 10px;
                          fill: black;"}}
        { :tag :line, :class :line
          :attr { :x1 (+ 40 x) :y1 :y :x2 width :y2 :y
                :stroke :stroke :stroke-dasharray :dash-array
                :style "stroke-width:1"}}]})

;; X-Axis
(defn- ch-x-axis [data-key x-start y width]
  { :tag :g
    :attr {:fill "none", :stroke "none"}
    :class :x :data data-key
    :nodes [
            ;; title
            {:tag :text, :class :x-title
              :attr { :x (/ (- width x-start) 2), :y (+ y 25)
                      :text-anchor "middle"
                      :style "font-size: 12px; 
                              fill: black;
                              font-weight: bold"}
              :text :title}
            ;; labels
            {:tag :text, :class :x-label
              :attr { :x :x, :y y
                      :text-anchor "middle"
                      :style "font-size: 10px;
                  fill: black;"}
              :text :text :multi? true :data :ticks}]})

;; Y-Axis
(defn- ch-y-axis [data-key x y-start height]
  { :tag :g
    :attr {:fill "none", :stroke "none"}
    :class :y :data data-key
    :nodes 
      [ ;; title
        {:tag :text, :class :y-title
          :attr { :x 0, :y (/ (- height y-start) 2), 
                  :text-anchor "middle"
                  :transform (str "rotate(270, 10, " (/ (- height y-start) 2) ")")
                  :style "font-size: 11px;
              fill: black;"}
          :text :title}
        ;; labels
        {:tag :g :class :y-labels
          :attr {:fill "none", :stroke "none"}
          :multi? true :data :ticks
          :nodes [{ :tag :line, :class :y-label-line
                    :attr { :x1 (+ 5 x) :y1 #(- (:y %) 5) :x2 (+ 25 x) :y2 #(- (:y %) 5)
                            :style "stroke-width:1;stroke:red"}}
                  { :tag :text, :class :y-label
                          :attr { :x (+ 5 x), :y #(- (:y %) 10)
                                  :style "font-size: 10px;
                              fill: black;"}
                          :text :text}]}]})

;; Shaded areas
(defn- ch-diagnol-shading [data-key] 
  { :tag :g :class :shaded
    :attr {:fill "none", :stroke "none"}
    :nodes 
      [{:tag :defs :class :defs 
        :nodes [{:tag :pattern :class :diagnol-pattern
                  :attr {:id "diagnol-pattern" 
                        :width 8 :height 10
                        :patternTransform "rotate(-45)"
                        :patternUnits "userSpaceOnUse"}
                  :nodes 
                    [{:tag :line :class :diagnol-pattern-line
                      :attr {:x1 0 :y1 0 :x2 0 :y2 10
                              :style "stroke:black; stroke-width:1"}}]}]}
       {:tag :g :class :shaded-area :data data-key :multi? true
        :attr {:fill "none", :stroke "none"}
        :nodes [{:tag :rect, :class :shaded-area-bound
                 :attr {:x :x :y :y :height :height :width :width
                        :style "stroke:grey; stroke-width:1" 
                        :fill "url(#diagnol-pattern)"}}]}]})
                              
(defn chart-layout 
  [{:keys [height width 
           point-events point-radius 
           renderers]
    {:keys [x-ps-os x-pe-os x-as-os x-ae-os
            y-ps-os y-pe-os y-as-os y-ae-os]} :bounds}]

  (let [empty-fn (constantly {:tag :g :class :empty})
        { :keys [ horizontal-bands-fn vertical-bands-fn
                  horizontal-lines-fn
                  x-axis-fn y-axis-fn
                  shaded-areas-fn
                  data-points-fn data-rects-fn
                  burners-fn tubes-fn] 
          :or { horizontal-bands-fn empty-fn
                vertical-bands-fn empty-fn
                horizontal-lines-fn empty-fn
                x-axis-fn empty-fn y-axis-fn empty-fn
                shaded-areas-fn empty-fn
                data-points-fn empty-fn
                data-rects-fn empty-fn
                burners-fn empty-fn tubes-fn empty-fn}} renderers]

    { :width width, :height height
      :view-box (str "0 0 " width " " height)
      :style {:color "white"
              ;:border "1px red solid"
              :font-size "32px"}
      
      :node {:tag :g :class :root
              :attr {:fill "none", :stroke "none"}
              :nodes [;; background
                      { :tag :g
                        :class :bg 
                        :attr {:fill "none", :stroke "none"}
                        :nodes [(horizontal-bands-fn width)
                                (vertical-bands-fn (- height y-ps-os))
                                
                                ;; borders
                                { :tag :line, :class :bg-top
                                   :attr {:x1 x-ps-os :y1 1 :x2 width :y2 1
                                          :style "stroke:red;stroke-width:1"}}
                                { :tag :line, :class :bg-bot
                                  :attr {:x1 x-ps-os :y1 (- height y-ps-os) :x2 width :y2 (- height y-ps-os)
                                         :style "stroke:grey;stroke-width:1"}}]}
                      
                      ;; data rectangles
                      (data-rects-fn)
                      ;; data points
                      (data-points-fn)
                      ;; horizontal lines
                      (horizontal-lines-fn (+ 5 x-ps-os) width)
                      ;; burners
                      (burners-fn)
                      ;; tubes
                      (tubes-fn)
                      ;; shaded areas
                      (shaded-areas-fn)
                      ;; x-axis
                      (x-axis-fn x-ps-os (+ 15 (- height y-ps-os)) width )
                      ;; y-axis
                      (y-axis-fn x-ps-os y-ps-os height)]}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Components ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn d3-chart [{:keys [data config]}]
  (let [state (r/atom {})]
    (r/create-class
      { :component-will-mount 
          (fn [this] nil)

        :reagent-render
        (fn [{:keys [data config]}]
            [:div {:style {:display "block"}}
              [d3-svg (assoc config 
                        :data data)]])})))





(defn overall-twt-chart [{:keys 
                  [height width red-firing title x-title y-title y-domain 
                   temp->color burner-nos tube-nos burner-domain]} 
                   {:keys [tube-data burner-data]}]
  (let [

        x-ps-os 20, x-pe-os 0, x-as-os 0, x-ae-os 0
        y-ps-os 50, y-pe-os 0, y-as-os  0, y-ae-os 0

        x-domain [1 (-> tube-data count inc)]
        x-range [(+ x-ps-os x-as-os) (- width x-pe-os x-ae-os)]

        y-domain y-domain 
        y-range [(- height y-ps-os y-as-os) (+ y-pe-os y-ae-os)]
        
        x-scale (d3-scale x-domain x-range)
        y-scale (d3-scale y-domain y-range)

        y-ticks (d3-ticks y-scale)

        x-axis {:ticks (mapv (fn [t] { :x (-> t :row-no (+ 0.5) x-scale) 
                                       :text (:name t)}) tube-data)}
        y-axis {:title y-title
                :ticks (mapv (fn [t] {:y (y-scale t) :text (str t)}) y-ticks)}
        
        x-tick-w (- (get-in x-axis [:ticks 1 :x]) (get-in x-axis [:ticks 0 :x]))
        y-tick-h (- (y-scale 1) (y-scale 2))

        h-bands (create-alt-bands (:ticks y-axis) x-ps-os)
        
        temperatures (flatten
            (mapv (fn [{st :start-tube et :end-tube 
                        row :row-no {:keys [a b]} :temperatures}]
                      (let [width (* 0.4 x-tick-w )]
                        (map (fn [l r t]
                          (let [y (- (y-scale t) y-tick-h)] [{:x (x-scale (+ 0.1 row)) :y y :height y-tick-h :width width :fill (temp->color l)}
                           {:x (x-scale (+ 0.5 row)) :y y :height y-tick-h :width width :fill (temp->color r)}])) 
                          a b (range st et)))) 
                  tube-data))

        tubes [{:fill "blue"
                :data (flatten 
                        (map (fn [{r :row-no st :start-tube et :end-tube}]
                          (let [x (x-scale (+ 0.5 r))]
                            (map (fn [t] {:x x :y (y-scale (+ t 0.5))}) (range st et)))) 
                          tube-data))}]

        xaxis {}
        burners {}
        horizontal-bands {}
        shaded-areas {}
        data {:temperatures temperatures
              :xaxis x-axis
              :burners burners
              :tubes tubes
              :horizontal-bands h-bands
              :shaded-areas shaded-areas}
        config 
          (assoc 
            (chart-layout 
              {:height height, :width width
               :bounds {:x-ps-os x-ps-os, :x-pe-os x-pe-os, 
                       :x-as-os x-as-os, :x-ae-os x-ae-os,
                       :y-ps-os y-ps-os, :y-pe-os y-pe-os, 
                       :y-as-os y-as-os, :y-ae-os y-ae-os}
               :renderers 
                { :horizontal-bands-fn 
                    (partial ch-horizontal-bands :horizontal-bands)
                  :x-axis-fn 
                    (partial ch-x-axis :xaxis)
                  :burners-fn 
                    (partial ch-points :burners 5 "grey" nil)
                  :tubes-fn 
                    (partial ch-points :tubes 2 "black" nil)
                  :data-rects-fn
                    (partial ch-data-rects :temperatures)
                  :shaded-areas-fn 
                    (partial ch-diagnol-shading :shaded-areas)}}) 
            :height height :width width)]
    (fn [] (let []
      (js/console.log x-tick-w)
            [:div {:style {:position "relative" :user-select "none"}} 
              [d3-chart {:data data :config config}]]))))


(defn twt-chart [{:keys 
                  [height width red-firing avg-temp-band avg-raw-temp 
                   avg-corr-temp x-title x-domain y-title y-domain
                   design-temp target-temp ]} data]
  
  (let [state (r/atom {})
    
        x-ps-os 20, x-pe-os 0, x-as-os 60, x-ae-os 0
        y-ps-os 50, y-pe-os 0, y-as-os  0, y-ae-os 0

        x-domain x-domain 
        x-range [(+ x-ps-os x-as-os) (- width x-pe-os x-ae-os)]
        
        y-domain y-domain 
        y-range [(- height y-ps-os y-as-os) (+ y-pe-os y-ae-os)]

        x-scale (d3-scale x-domain x-range)
        y-scale (d3-scale y-domain y-range)
    
        x-ticks (d3-ticks x-scale)
        y-ticks (d3-ticks y-scale)

        x-axis {:title x-title 
                :ticks (mapv (fn [t] {:x (x-scale t) :text (format-to-n t 2)}) x-ticks)}

        y-axis {:title y-title
                :ticks (mapv (fn [t] {:y (y-scale t) :text (str t)}) y-ticks)}
    
        p-keys [{:kname :a, :fill "blue"}
                {:kname :b, :fill "red"}]

        points (map (fn [{:keys [kname fill]}]
                      {:fill fill
                       :data (map (fn [d] {:x (x-scale (:tube d))
                                             :y (y-scale (kname d))}) 
                                                data)}) p-keys)

        h-lines [{:y (y-scale design-temp), :stroke "red", :text "Design temp"}
                 {:y (y-scale avg-corr-temp), :stroke "blue", :text ""}
                 {:y (y-scale avg-raw-temp), :stroke "black", :text "" 
                  :dash-array "5, 5"}]

        h-bands (merge 
                  (create-alt-bands (:ticks y-axis) x-ps-os)
                  (let [[from to] avg-temp-band
                        from-p (y-scale from) to-p (y-scale to)] 
                    {:x x-ps-os :y to-p :height (- from-p to-p) :fill "pink"}))

        v-bands (mapv (fn [[sr er]]
                         (let [sp (x-scale (- sr 0.5))
                               ep (x-scale (+ er 0.5))]
                            {:x sp, :width (- ep sp) 
                              :fill "rgba(10,10,10,0.3)"})) red-firing)
        
        p-events {:mouseover (fn [%1 %2] (swap! state assoc :popup %2))
                  :mouseout (fn [_] (swap! state assoc :popup nil))}
        
        data {:points points
              :yaxis y-axis
              :xaxis x-axis
              :hor-lines h-lines
              :horizontal-bands h-bands
              :vertical-bands v-bands}
        config 
          (assoc 
            (chart-layout 
              {:point-events p-events
              :height height, :width width
              :bounds {:x-ps-os x-ps-os, :x-pe-os x-pe-os, 
                       :x-as-os x-as-os, :x-ae-os x-ae-os,
                       :y-ps-os y-ps-os, :y-pe-os y-pe-os, 
                       :y-as-os y-as-os, :y-ae-os y-ae-os}
              :point-radius 3
              :renderers 
                { :horizontal-bands-fn 
                    (partial ch-horizontal-bands :horizontal-bands)
                  :vertical-bands-fn 
                    (partial ch-vertical-bands :vertical-bands)
                  :horizontal-lines-fn 
                    (partial ch-horizontal-lines :hor-lines)
                  :x-axis-fn 
                    (partial ch-x-axis :xaxis)
                  :y-axis-fn 
                    (partial ch-y-axis :yaxis)
                  :data-points-fn 
                    (partial ch-points :points 3 "blue" p-events)}}) 
            :height height :width width)]

    (fn [] (let [popup (:popup @state)]
            [:div {:style {:position "relative" :user-select "none"}} 
              [d3-chart {:data data :config config}]
              (if popup [:div {:style {:font-size 10, :border "1px lightblue solid", 
                                       :padding 10
                                       :color "white" :background-color "rgba(10,10,10,0.7)"
                                       :border-radius 3, :position "absolute", :width 120 
                                       :left (+ 3 (:x popup))
                                       :top (+ 3 (:y popup))}} 
                         (:content popup)])]))))



(defn chart-layout2 [{
                      :keys [height width bgevents]}]
  
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
                    :attr {:x 0, :y 0, :width width, :height height, :fill "aliceblue"}}
          
                  ;; zoom area
                  {:tag :rect, :class :zoom
                    :attr {:x :x, :y :y, :width :width, :height :height, :fill "pink"}
                    :data :zoom}
          
                  ;; plot
                  {:tag :g
                    :attr {:fill "none", :stroke "none"}
                    :class :series
                    :nodes [
                            {:tag :circle, :class :point
                              :attr {:r 3 :cx :x, :cy :y 
                                      :fill "red"}
                              :multi? true
                              :data :points}]}]}})

(defn d3-chart2 [{:keys [data config zoom-fn]}]
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
                      (if (get-in @state [:modes :zoom]) 
                        (swap! state update-in [:bg :selected] 
                          (fn [selected]
                              (if selected 
                                (let [ cx js/d3.event.offsetX cy js/d3.event.offsetY 
                                      w (abs (- cx (:x0 selected))) h (abs (- cy (:y0 selected)))
                                      x (min (:x0 selected) cx) y (min (:y0 selected) cy)]
                                     (assoc selected :width w :height h :x x :y y))))))

                      (if (:panning? @state) 
                        (do (swap! state update-in [:bg :zoom-area] 
                             (fn [zoom-area]
                                 (if zoom-area (let 
                                                [cx js/d3.event.offsetX cy js/d3.event.offsetY
                                                 {sx :x sy :y} (get-in @state [:bg :selected])
                                                 w (- cx sx) h (- cy sy)
                                                 lx (or (:x0 zoom-area) (:x zoom-area))
                                                 ly (or (:y0 zoom-area) (:y zoom-area))
                                                 x (- lx w) y (- ly h)]
                                                (assoc zoom-area :x x :y y :x0 lx :y0 ly))))
                    
                  
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
                    (swap! state assoc :panning? false))
          

        bg-mouseleave (fn [ev]
                       (let [x js/d3.event.offsetX
                             y js/d3.event.offsetY]
                         (if (not (and (<= 0 x width) (<= 0 y height))) 
                           (swap! state assoc-in [:bg :selected] nil))
                         (swap! state assoc :panning? false)))

        layout (chart-layout2 {
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
