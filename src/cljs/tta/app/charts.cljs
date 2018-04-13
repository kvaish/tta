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

(defn linear-scale [[x1 x2] [y1 y2] data]
  ;; scale data from domain to range
  (let [factor (/ (- y2 y1) (- x2 x1))]
    (if (seqable? data) (mapv #(+ y1 (* factor (- % x1))) data)
     (+ y1 (* factor (- data x1))))))

(defn gen-chart [n]
  (let [xi 0, xe 500
        yi 0, ye 300
        n (or n 100)
        x (map #(* % (/ xe n)) (random-sample 0.5 (range n)))
        f (fn [x xi yi] (+ (* x (/ yi xi)) (rand-int 50)))]
    (mapv (fn [x]
            {:x (+ x 60)
             :y (- 260 (f x xe ye))}) x)))

(defn chart-layout [{:keys [height width point-events point-radius]
                     [x-reserve y-reserve] :axis-size}]
  (let [band-st (/ x-reserve 2)
        plot-ym (- height y-reserve)]
    { :width width, :height height
      :view-box (str "0 0 " width " " height)
      :style {:color "white"
              :font-size "32px"}
      :node {:tag :g
              :attr {:fill "none", :stroke "none"}
              :class :root
              :nodes [
                      ;; background
                      { :tag :g
                        :class :bg 
                        :attr {:fill "none", :stroke "none"}
                        :nodes [
                                ;; horizontal bands
                                { :tag :rect, :class :bg-hor-bands
                                  :attr {:x band-st :y :y :height :height :width width :fill :fill}
                                  :data :hor-bands
                                  :multi? true}
                                ;; vertical bands
                                { :tag :rect, :class :bg-ver-bands
                                  :attr {:x :x :y :0 :height plot-ym :width :width :fill :fill}
                                  :data :ver-bands
                                  :multi? true}
                                  ;; borders
                                { :tag :line, :class :bg-top
                                   :attr {:x1 band-st :y1 1 :x2 width :y2 1
                                          :style "stroke:red;stroke-width:1"}}
                                { :tag :line, :class :bg-bot
                                  :attr {:x1 band-st :y1 plot-ym :x2 width :y2 plot-ym
                                         :style "stroke:red;stroke-width:1"}}]}
                    
                      ;; plot
                      { :tag :g
                        :attr {:fill "none", :stroke "none"}
                        :class :series
                        :data :plot
                        :nodes [
                                ;; horizontal lines
                                {:tag :g :class :hor-lines
                                 :attr {:fill "none", :stroke "none"}
                                 :multi? true :data :hor-lines
                                 :nodes [
                                        ;; title
                                         {:tag :text, :class :line-title
                                           :text :text
                                           :attr { :x (+ 5 x-reserve), :y #(- (:y %) 5)
                                                   :style "font-size: 10px;
                                      fill: black;"}}
                                         {:tag :line, :class :line
                                           :attr { :x1 (+ 5 x-reserve) :y1 :y :x2 width :y2 :y
                                                  :stroke :stroke :stroke-dasharray :dash-array
                                                  :style "stroke-width:1"}}]}
                                ;; point
                                {:tag :circle, :class :point
                                  :attr {:r point-radius :cx :x, :cy :y 
                                          :fill "red"}
                                  :multi? true
                                  :on point-events                      
                                  :data :points}]}
                
                      ;; x-axis
                      { :tag :g
                        :attr {:fill "none", :stroke "none"}
                        :class :x :data :xaxis
                        :nodes [
                                ;; title
                                {:tag :text, :class :x-title
                                  :attr { :x (/ (- width band-st) 2), :y height
                                          :text-anchor "middle"
                                          :style "font-size: 12px; 
                                                  fill: black;
                                                  font-weight: bold"}
                                  :text :title}
                                ;; labels
                                {:tag :text, :class :x-label
                                  :attr { :x :x, :y #(+ 15 plot-ym)
                                         :text-anchor "middle"
                                          :style "font-size: 10px;
                                      fill: black;"}
                                      
                                  :text :text :multi? true :data :points}]}

                      ;; y-axis
                      { :tag :g
                        :attr {:fill "none", :stroke "none"}
                        :class :y :data :yaxis
                        :nodes [
                                ;; title
                                {:tag :text, :class :y-title
                                  :attr { :x 0, :y (/ plot-ym 2), 
                                         :text-anchor "middle"
                                         :transform (str "rotate(270, 10, " (/ plot-ym 2) ")")
                                          :style "font-size: 11px;
                                      fill: black;"}
                                  :text :title}
                                ;; labels
                                {:tag :g :class :y-labels
                                 :attr {:fill "none", :stroke "none"}
                                 :multi? true :data :points
                                  :nodes [{ :tag :line, :class :y-label-line
                                            :attr { :x1 (+ 5 band-st) :y1 :y :x2 (+ 25 band-st) :y2 :y
                                                   :style "stroke-width:1;stroke:red"}}
                                          { :tag :text, :class :y-label
                                                  :attr { :x (+ 5 band-st), :y #(- (:y %) 5)
                                                          :style "font-size: 10px;
                                                      fill: black;"}
                                                  :text :text}]}]}]}}))

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

(defn twt-chart [{:keys 
                  [height width red-firing avg-temp-band avg-raw-temp 
                   avg-corr-temp title x-title x-domain y-title y-domain
                   design-temp target-temp]} data]
  (let [
        state (r/atom {})
        chart-data {}
        x-reserve 50
        y-reserve 40
        x-range [x-reserve width]
        y-range [y-reserve height]
        scale-x (partial linear-scale x-domain x-range)
        scale-y (partial linear-scale y-domain y-range)
        data->points 
        (fn [x-domain x-range y-domain y-range data] 
            (flatten 
             (mapv (fn [{:keys [tube-no temperatures]}]
                       (let [x (scale-x tube-no)]
                         (map (fn [temp] 
                               {:x x :y (- height (scale-y temp)) 
                                :content (str "Tube: ", tube-no, " Temperature: " temp)}) 
                              temperatures))) 
                   data)))
        plot {:points (data->points x-domain x-range y-domain y-range data)
              :hor-lines [{:y (- height (scale-y design-temp))
                           :stroke "red" :text "Design temp"}
                          {:y (- height (scale-y avg-corr-temp))  
                           :stroke "blue" :text ""}
                          {:y (- height (scale-y avg-raw-temp)) 
                           :stroke "black" :text "" :dash-array "5, 5"}]}
        yaxis { :title y-title
                :points (let 
                         [axis-pts (drop 1 (take-nth 10 
                                            (range (first y-domain) (last y-domain))))
                          scaled-pts (scale-y axis-pts)]
                         (mapv (fn [t y] {:y (- height y) 
                                          :text (str t)}) axis-pts scaled-pts))}
        xaxis { :title x-title
                :points (let [axis-pts (drop 1 (take-nth 1
                                                (range (first x-domain) (last x-domain))))
                              scaled-pts (scale-x axis-pts)]
                         (mapv (fn [r x] {:x x 
                                          :text (str r)}) axis-pts scaled-pts))}
        point-radius 3
        point-events {:mouseover (fn [%1 %2]
                                     (swap! state assoc :popup %2))
                           :mouseout (fn [_]
                                       (swap! state assoc :popup nil))}
        
        hor-bands [(let [[from to] avg-temp-band 
                         from-p (- height (scale-y from))
                         to-p (- height (scale-y to))] 
                        {:y to-p :height (- from-p to-p) :fill "pink"})]
        ver-bands (mapv (fn [[start-r end-r]]
                         (let [start-p (scale-x (- start-r 0.5))
                               end-p (scale-x (+ end-r 0.5))]
                              {:x start-p :width (- end-p start-p) 
                               :fill "rgba(10,10,10,0.3)"})) red-firing)
        
        data (assoc chart-data 
              :plot plot
              :yaxis yaxis
              :xaxis xaxis
              :hor-bands hor-bands
              :ver-bands ver-bands)
        config (assoc (chart-layout 
                       {:point-events point-events
                        :height height, :width width
                        :point-radius point-radius
                        :axis-size [x-reserve y-reserve]}) 
                :height height :width width)]

    (fn [] (let [popup (:popup @state)]
            [:div {:style {:position "relative" :user-select "none"}} 
              [d3-chart {:data data :config config}]
              (if popup [:div {:style {:font-size 10, :border "1px lightblue solid", 
                                       :padding 10
                                       :color "white" :background-color "rgba(10,10,10,0.7)"
                                       :border-radius 3, :position "absolute", :width 120 
                                       :left (+ point-radius (:x popup))
                                       :top (+ point-radius (:y popup))}} 
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
