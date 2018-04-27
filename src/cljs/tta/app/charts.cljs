(ns tta.app.charts
  (:require [clojure.set :as set]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [reagent.dom :as dom]
            [cljsjs.d3]
            [stylefy.core :as stylefy]
            [ht.style :as ht-style :refer [color-hex color-rgba]]
            [ht.util.interop :as i]
            [ht.util.common :refer [tooltip-pos save-svg-to-file]]
            [ht.app.subs :refer [translate]]
            [tta.util.common :refer [to-temp-unit from-temp-unit deg-C]]
            [tta.app.d3 :refer [d3-svg d3-svg->string]]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.subs :as app-subs]))

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
  (let [scale (-> js/d3
                  (i/ocall :scaleLinear)
                  (i/ocall :domain (to-array domain))
                  (i/ocall :range (to-array range)))]
    scale))

(defn d3-ticks [scale]
  (vec (i/ocall scale :ticks)))

(defn- abs [n] (if (pos? n) n (- n)))

(defn- data-alt-horizontal-bands [x w ys color]
  (mapv (fn [[y1 y2]]
          {:x x, :y (min y1 y2)
           :w w, :h (abs (- y1 y2))
           :color color})
        (partition 2 ys)))

(defn- data-alt-vertical-bands [y h xs color]
  (mapv (fn [[x1 x2]]
          {:x (min x1 x2), :y y
           :w (abs (- x1 x2)), :h h
           :color color})
        (partition 2 xs)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Layouts ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ch-defs-radial-fade [id class-kw color opacity-0 opacity-100]
  {:tag :defs, :class :grad-defs
   :nodes [{:tag :radialGradient, :class :rg
            :attr {:id id, :r "50%"
                   :cx "50%", :cy "50%"
                   :fx "50%", :fy "50%"}
            :nodes [{:tag :stop, :class :stop-0
                     :attr {:offset "0%"
                            :style (str "stop-color:" color
                                        ";stop-opacity:" opacity-0)}}
                    {:tag :stop, :class :stop-100
                     :attr {:offset "100%"
                            :style (str "stop-color:" color
                                        ";stop-opacity:" opacity-100)}}]}]})

(defn- ch-defs-diagonal-lines [id class-kw color spacing]
  {:tag :defs, :class class-kw
   :nodes [{:tag :pattern, :class :pattern
            :attr {:id id
                   :width spacing :height 10
                   :patternTransform "rotate(-45)"
                   :patternUnits "userSpaceOnUse"}
            :nodes [{:tag :line :class :line
                     :attr {:x1 0 :y1 0 :x2 0 :y2 10
                            :stroke color}}]}]})

(defn- ch-point-shadow [class-kw data-key fill-id size]
  {:tag :circle, :class class-kw
   :data data-key
   :skip? #(not (:x %))
   :attr {:r (/ size 2), :cx :x, :cy :y
          :fill (str "url(#" fill-id ")")}})

(defn- vertical-text [{:keys [x y]}]
  (str "rotate(270, " x ", " y ")"))

;; Bands
(defn- ch-bands [class-kw data-key]
  { :tag :rect, :class class-kw
    :attr {:x :x, :y :y, :height :h, :width :w, :fill :color}
    :data data-key
    :multi? true})

;; Markers

(defn- ch-marker-circle [class-kw data-key size color events]
  {:tag :circle, :class class-kw
   :attr {:r (/ size 2), :cx :x, :cy :y
          :fill color, :stroke "none"}
   :multi? true
   :data data-key
   :on events})

(defn- ch-marker-square [class-kw data-key size color events]
  {:tag :rect, :class class-kw
   :attr {:x #(- (:x %) (/ size 2))
          :y #(- (:y %) (/ size 2))
          :width size, :height size
          :fill color, :stroke "none"}
   :multi? true
   :data data-key
   :on events})

;; Rectangles
(defn- ch-rects [class-kw data-key]
  {:tag :rect, :class class-kw
   :attr {:x :x, :y :y
          :height :h, :width :w
          :stroke :color, :fill :color}
   :multi? true
   :data data-key})

;; Horizontal lines
(defn- ch-horizontal-lines [class-kw data-key]
  {:tag :g, :class class-kw
   :multi? true
   :data data-key
   :nodes
   [;; title
    {:tag :text, :class :label
     :data :label
     :text :text
     :attr {:x :x, :y :y
            :style (str "font-size:10px; fill:" (color-hex :bitumen-grey))}}
    {:tag :line, :class :line
     :data :line
     :attr {:x1 :x1, :y1 :y, :x2 :x2, :y2 :y
            :shape-rendering "crispEdges"
            :stroke-dasharray #(if (:dashed? %) "5,5")
            :stroke :color}}]})

;; Walls
(defn- ch-walls [class-kw data-key]
  (let [attr-h {:style (str "font-size:12px; fill:" (color-hex :bitumen-grey))
                :text-anchor "middle"}
        attr-v (assoc attr-h :transform vertical-text)]
    {:tag :g, :class class-kw
     :data data-key
     :nodes [{:tag :text, :class :west
              :data :west, :text :text
              :attr attr-v}
             {:tag :text, :class :north
              :data :north, :text :text
              :attr attr-h}
             {:tag :text, :class :east
              :data :east, :text :text
              :attr attr-v}
             {:tag :text, :class :south
              :data :south, :text :text
              :attr attr-h}]}))

;; X-Axis
(defn- ch-x-axis [class-kw data-key]
  {:tag :g, :class class-kw
   :data data-key
   :nodes [;; title
           {:tag :text, :class :x-title
            :data :title
            :attr {:x :x, :y :y
                   :text-anchor "middle"
                   :style (str "font-size: 12px; font-weight: bold; fill:"
                               (color-hex :bitumen-grey))}
            :text :text}
           ;; labels
           {:tag :text, :class :x-label
            :multi? true
            :data :ticks
            :attr {:x :x, :y :y
                   :text-anchor "middle"
                   :style (str "font-size: 10px; fill:"
                               (color-hex :royal-blue))}
            :text :text}]})

(defn- data-x-axis [x-title x-scale x-ticks x-range y x-label-fn]
  {:title {:x (/ (apply + x-range) 2)
           :y (+ y 32)
           :text x-title}
   :ticks (map (fn [ti]
                 {:x (x-scale ti), :y (+ y 14)
                  :text (x-label-fn ti)})
               x-ticks)})

;; Y-Axis
(defn- ch-y-axis [class-kw data-key tick-width]
  {:tag :g, :class class-kw
   :data data-key
   :nodes [ ;; title
           {:tag :text, :class :y-title
            :data :title
            :attr {:x :x, :y :y
                   :text-anchor "middle"
                   :transform vertical-text
                   :style (str "font-size: 12px; font-weight: bold; fill:"
                               (color-hex :bitumen-grey))}
            :text :text}
           ;; labels
           {:tag :g, :class :y-label
            :multi? true
            :data :ticks
            :nodes [{:tag :line, :class :line
                     :attr {:x1 :x, :y1 :y, :x2 #(+ (:x %) tick-width), :y2 :y
                            :shape-rendering "crispEdges"
                            :stroke (color-hex :royal-blue)}}
                    {:tag :text, :class :y-label
                     :attr {:x :x, :y #(- (:y %) 5)
                            :style (str "font-size: 10px; fill:"
                                        (color-hex :royal-blue))}
                     :text :text}]}]})

(defn- data-y-axis [y-title y-scale y-ticks y-range x y-label-fn]
  {:title {:x (- x 10)
           :y (/ (apply + y-range) 2)
           :text y-title}
   :ticks (->> (map (fn [ti]
                      {:x (+ x 10), :y (y-scale ti)
                       :text (y-label-fn ti)})
                    y-ticks)
               (filter (fn [{:keys [y]}]
                         (> (- y 14) (second y-range)))))})

;; Shaded areas
(defn- ch-diagonal-shading [class-kw data-key fill-id color]
  {:tag :rect, :class class-kw
   :data data-key
   :multi? true
   :attr {:x :x :y :y :height :height :width :width
          :style (str "stroke-width:1;stroke:" color)
          :fill (str "url(#" fill-id ")")}})

#_(defn chart-layout 
  [{:keys [height width 
           point-events point-radius 
           renderers]
    {:keys [x-ps-os x-pe-os x-as-os x-ae-os
            y-ps-os y-pe-os y-as-os y-ae-os]} :bounds}]

  (let [empty-fn (constantly {:tag :g :class :empty})
        { :keys [ horizontal-bands-fn vertical-bands-fn
                  horizontal-lines-fn
                  x-axis-fn y-axis-fn
                  walls-fn shaded-areas-fn
                  data-points-fn data-rects-fn
                  burners-fn tubes-fn] 
          :or { horizontal-bands-fn empty-fn
                vertical-bands-fn empty-fn
                horizontal-lines-fn empty-fn
                x-axis-fn empty-fn y-axis-fn empty-fn
                shaded-areas-fn empty-fn
                data-points-fn empty-fn
                walls-fn empty-fn
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
                                   :attr {:x1 x-ps-os :y1 y-pe-os :x2 width :y2 y-pe-os
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
                      ;; walls
                      (walls-fn)
                      ;; x-axis
                      (x-axis-fn x-ps-os (+ 15 (- height y-ps-os)) width )
                      ;; y-axis
                      (y-axis-fn x-ps-os y-ps-os height)]}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Components ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; OVERALL-TWT-CHART;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn update-overall-twt-data [state props]
  (let [state (merge state props)
        {:keys [max-temp min-temp
                height width
                burner-on? temps]}

        ;; plot => chamber box, axes => color plots
        x-ps-os 20, x-pe-os 0, x-as-os 20, x-ae-os 20
        y-ps-os 40, y-pe-os 25, y-as-os 0, y-ae-os 0
        ]))

(defn overall-twt-chart [{:keys [;; static
                                 wall-names row-names
                                 max-temp min-temp
                                 ;; dynamic
                                 height width
                                 burner-on? temps]}]
  (let [

        x-domain [1 (-> tube-data count inc)]
        x-range [(+ x-ps-os x-as-os) (- width x-pe-os x-ae-os)]

        y-domain y-domain 
        y-range [(- height y-ps-os y-as-os) (+ y-pe-os y-ae-os)]
        
        x-scale (d3-scale x-domain x-range)
        y-scale (d3-scale y-domain y-range)

        y-ticks (d3-ticks y-scale)

        x-axis {:ticks (mapv (fn [t] { :x (-> t :row-no (+ 0.5) x-scale) 
                                       :text (:name t)}) tube-data)}
        y-axis {:ticks (mapv (fn [t] {:y (y-scale t) :text (str t)}) y-ticks)}
        
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

        burners [{:fill "green"
                :data (flatten 
                        (map-indexed (fn [ i {c :burner-count sb :start-burner eb :end-burner}]
                          (let [b-scale (d3-scale [1 c] y-range)
                                x (x-scale (+ (if burner-first? 1 2) i))]
                            (map (fn [b] {:x x :y (b-scale (+ b 0.5))}) (range sb eb)))) 
                          burner-data))}]

        ;; TODO - Convert reduced firing data to shaded areas
        shaded-areas [{:x 120 :y 100 :height 100 :width 160}
                      {:x 440 :y 200 :height 50 :width 160}]
        data {:temperatures temperatures
              :xaxis x-axis
              :burners burners
              :walls wall-names
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
                  :walls-fn (partial ch-walls 
                                    :walls {:west {:x (+ 15 x-ps-os) :y (/ height 2)}
                                            :east {:x (- width x-pe-os 20) :y (/ height 2)}
                                            :north {:x (/ width 2) :y 10}
                                            :south {:x (/ width 2) :y (- height 10)}})
                  :data-rects-fn
                    (partial ch-data-rects :temperatures)
                  :shaded-areas-fn 
                    (partial ch-diagnol-shading :shaded-areas)}}) 
            :height height :width width)]
    (fn [] (let []
      (js/console.log burners)
            [:div {:style {:position "relative" :user-select "none"}} 
              [d3-chart {:data data :config config}]]))))


;; TWT-CHART ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn burner-bands [burner-on?]
  (->> (partition-by
        second
        (->> burner-on?
             (map-indexed list)))
       (remove (comp second first))
       (map (fn [bs]
              (let [bi (ffirst bs)
                    be (first (last bs))]
                [(- bi 0.5) (+ be 0.5)])))))

(defn twt-chart-legend [color-1 color-2]
  (let [label-style (str "font-size:10px;fill:" (color-hex :bitumen-grey))
        f (fn [kw os] #(+ (kw %) os))]
    {:tag :g, :class :legend
     :data :legend
     :nodes [{:tag :g, :class :marker-1
              :nodes [{:tag :circle, :class :marker-circle
                       :attr {:r 3, :cx (f :x 6), :cy (f :y 6)
                              :fill color-1}}
                      {:tag :text, :class :side-left
                       :attr {:style label-style
                              :x (f :x 18), :y (f :y 9)}
                       :text [:labels :side-names 0]}]}
             {:tag :g, :class :marker-2
              :nodes [{:tag :rect, :class :marker-rect
                       :attr {:width 6, :height 6
                              :x (f :x 60), :y (f :y 3)
                              :fill color-2}}
                      {:tag :text, :class :side-right
                       :attr {:style label-style
                              :x (f :x 78), :y (f :y 9)}
                       :text [:labels :side-names 1]}]}
             {:tag :g, :class :avg-temp
              :nodes [{:tag :line, :class :line
                       :attr {:x1 (f :x 150), :x2 (f :x 170)
                              :y1 (f :y 6), :y2 (f :y 6)
                              :stroke (color-hex :ocean-blue)
                              :shape-rendering "crispEdges"}}
                      {:tag :text, :class :label
                       :attr {:style label-style
                              :x (f :x 175), :y (f :y 9)}
                       :text [:labels :avg-temp]}]}
             {:tag :g, :class :avg-raw-temp
              :nodes [{:tag :line, :class :line
                       :attr {:x1 (f :x 250), :x2 (f :x 270)
                              :y1 (f :y 6), :y2 (f :y 6)
                              :stroke (color-hex :ocean-blue)
                              :shape-rendering "crispEdges"
                              :stroke-dasharray "5,5"}}
                      {:tag :text, :class :label
                       :attr {:style label-style
                              :x (f :x 275), :y (f :y 9)}
                       :text [:labels :avg-raw-temp]}]}
             {:tag :g, :class :reduced-firing
              :nodes [{:tag :rect, :class :rect
                       :attr {:x (f :x 350), :y :y, :width 20, :height 12
                              :fill (color-rgba :red 30 0.5)}}
                      {:tag :text, :class :label
                       :attr {:style label-style
                              :x (f :x 375), :y (f :y 9)}
                       :text [:labels :reduced-firing]}]}
             {:tag :g, :class :avg-temp-band
              :nodes [{:tag :rect, :class :rect
                       :attr {:x (f :x 450), :y :y, :width 20, :height 12
                              :fill (color-rgba :ocean-blue 50 0.5)}}
                      {:tag :text, :class :label
                       :attr {:style label-style
                              :x (f :x 475), :y (f :y 9)}
                       :text [:labels :avg-temp-band]}]}]}))

(defn update-twt-data [state props]
  (let [state (merge state props)
        {:keys [max-temp min-temp
                start-tube end-tube
                design-temp target-temp
                height width
                burner-on?
                reduced-firing-bands avg-temp-band
                avg-raw-temp avg-temp temps
                temp-unit]} state

        x-ps-os 30, x-pe-os 5, x-as-os 40
        y-ps-os 40, y-pe-os 24
        plot-x-start x-ps-os
        plot-x-width (- width x-ps-os x-pe-os)
        plot-x-end (- width x-pe-os)

        tube-count (inc (abs (- end-tube start-tube)))
        burner-count (count burner-on?)
        reduced-firing-bands (and burner-on?
                                  ;; evaluate once only, since not going to change
                                  (or reduced-firing-bands
                                      (burner-bands burner-on?)))
        [max-temp min-temp] (let [ts (filter pos? [max-temp min-temp
                                                   design-temp target-temp
                                                   avg-raw-temp avg-temp])
                                  maxt (apply max ts)
                                  mint (apply min ts)
                                  dt (* 0.2 (- maxt mint))]
                              (->> [(+ maxt dt) (- mint dt)]
                                   (map js/Math.round)))
        ;; axes domain & range
        x-domain [-1 tube-count]
        x-range [(+ x-ps-os x-as-os) (- width x-pe-os)]
        y-domain [min-temp max-temp]
        y-range [(- height y-ps-os) y-pe-os]
        ;; axes scales & ticks
        x-scale (d3-scale x-domain x-range)
        y-scale (d3-scale y-domain y-range)
        x-ticks (filter #(< -1 % tube-count) (d3-ticks x-scale))
        y-ticks (->> (d3-ticks (as-> (map #(to-temp-unit % temp-unit) y-domain) $
                                 (d3-scale $ y-range)))
                     (map #(from-temp-unit % temp-unit)))
        ;; plottable area x/y sizes
        x-start (x-scale -0.5)
        x-end (x-scale (- tube-count 0.5))
        x-width (- x-end x-start)
        y-start (second y-range)
        y-end (first y-range)
        y-height (- y-end y-start)
        ;; axes label fn
        x-label-fn #(if (> end-tube start-tube)
                      (+ start-tube %)
                      (- start-tube %))
        y-label-fn #(to-temp-unit % temp-unit)
        ;; axes data
        x-axis (data-x-axis "x-title" x-scale x-ticks x-range y-end x-label-fn)
        y-axis (data-y-axis "y-title" y-scale
                            (filter #(< min-temp % max-temp) y-ticks)
                            y-range plot-x-start y-label-fn)
        ;; burner x-axis scale
        b-domain [-0.5 (- burner-count 0.5)]
        b-range [x-start x-end]
        b-scale (d3-scale b-domain b-range)

        ;; points data
        points (vec (map-indexed (fn [si temps]
                                   (->> temps
                                        (map-indexed (fn [i t]
                                                       (if t
                                                         {:x (x-scale i)
                                                          :y (y-scale t)
                                                          :si si, :ti i, :t t})))
                                        (remove nil?)
                                        vec))
                                 temps))

        ;; horizontal lines data
        [dlo tlo] (if (and design-temp target-temp
                           (> 15 (abs (- (y-scale design-temp)
                                         (y-scale target-temp)))))
                    (if (> design-temp target-temp) [-5 10] [10 -5])
                    [-5 -5])
        h-lines (->> [[design-temp (color-hex :red -20)
                       (translate [:twt-chart :line-label :design-temp] "Design temp")
                       false dlo]
                      [target-temp (color-hex :green -20)
                       (translate [:twt-chart :line-label :target-temp] "Target temp")
                       false tlo]
                      [avg-temp (color-hex :ocean-blue) nil false 0]
                      [avg-raw-temp (color-hex :ocean-blue) nil true 0]]
                     (filter first)
                     (mapv (fn [[temp color text dashed? oy]]
                             (let [y (y-scale temp)]
                               {:label {:x (+ 10 x-start)
                                        :y (+ y oy)
                                        :text text}
                                :line {:x1 x-start
                                       :x2 x-end
                                       :y y, :color color
                                       :dashed? dashed?}}))))

        ;; border lines
        h-lines-bg (->> y-range
                        (mapv (fn [y]
                                {:label {:x 0, :y 0}
                                 :line {:x1 plot-x-start
                                        :x2 plot-x-end
                                        :y y
                                        :color (color-hex :royal-blue)}})))

        ;; background horizontal bands
        h-bands-bg (data-alt-horizontal-bands plot-x-start plot-x-width
                                              (map y-scale
                                                   (concat [(first y-domain)]
                                                           y-ticks
                                                           [(second y-domain)]))
                                              (color-hex :sky-blue 80))

        ;; avg temp bands
        h-bands-avg (let [[from to] avg-temp-band
                          from-p (y-scale from)
                          to-p (y-scale to)]
                      [{:x x-start
                        :y (min from-p to-p)
                        :w x-width
                        :h (abs (- from-p to-p))
                        :color (color-rgba :ocean-blue 50 0.5)}])

        ;; reduced firing bands
        v-bands (mapv (fn [[bi be]]
                        (let [bip (b-scale bi)
                              bep (b-scale be)]
                          {:x bip, :y y-start
                           :w (- bep bip)
                           :h y-height
                           :color (color-rgba :red 30 0.5)}))
                      reduced-firing-bands)

        ;; legend position
        legend  {:x plot-x-start, :y (- (/ y-start 2) 6)}]

    (-> state
        (merge props)
        (assoc :data {:points points
                      :y-axis y-axis
                      :x-axis x-axis
                      :h-lines h-lines
                      :h-lines-bg h-lines-bg
                      :h-bands-bg h-bands-bg
                      :h-bands-avg h-bands-avg
                      :v-bands v-bands
                      :legend legend}
               :reduced-firing-bands reduced-firing-bands
               :x-label-fn x-label-fn
               :y-label-fn y-label-fn))))

(defn twt-popup [{:keys [state tooltip-state]}]
  (let [{:keys [side-names x-label-fn y-label-fn]} @state
        {:keys [x y si ti t] :as tooltip} @tooltip-state
        w 80, h 50
        {:keys [x y]} (tooltip-pos {:tw w, :th h
                                    :ax (- x 3), :ay (- y 3)
                                    :aw 6, :ah 6
                                    :adx 11, :tdx 0, :tym 5})]
    (if tooltip
      [:div {:style {:width w, :height h
                     :padding 0
                     :background (color-hex :white)
                     :border (str "1px solid " (color-hex :bitumen-grey))
                     :border-radius "6px"
                     :position "absolute"
                     :left x, :top y}}
       [:svg {:view-box (str "0 0 " w " " h)
              :width w, :height h
              :style {:font-size "10px"
                      :margin 0
                      :color (color-hex :bitumen-grey)}}
        (if (zero? si)
          [:circle {:r 3, :cx 10, :cy 25
                    :fill (color-hex :royal-blue)}]
          [:rect {:x 7, :y 22, :width 6, :height 6
                  :fill (color-hex :royal-blue 40)}])
        [:text {:x 20, :y 15, :fill "currentColor"}
         (x-label-fn ti)]
        [:text {:x 20, :y 28, :fill "currentColor"}
         (get side-names si)]
        [:text {:x 20, :y 41, :fill "currentColor"}
         (str (y-label-fn t) " " @(rf/subscribe [::app-subs/temp-unit]))]]])))

(defn twt-chart-d3-config [state config
                           {:keys [y-title x-title legend-labels tooltip]}]
  (let [{:keys [data width height]} state
        data (-> data
                 (assoc-in [:pt-shadow] tooltip)
                 (assoc-in [:y-axis :title :text] y-title)
                 (assoc-in [:x-axis :title :text] x-title)
                 (assoc-in [:legend :labels] legend-labels))]
    (assoc config :data data
           :view-box (str "0 0 " width " " height)
           :width width, :height height)))

(defn twt-chart-export [state config {:keys [y-title x-title legend-labels]}]
  (let [props {:width 700, :height 500}
        {:keys [width height]} props
        state (update-twt-data state props)
        d3-config (twt-chart-d3-config state config {:x-title x-title
                                                     :y-title y-title
                                                     :legend-labels legend-labels})
        svg-string (d3-svg->string d3-config)]
    (save-svg-to-file "chart.png" svg-string width height 30)))

(defn twt-chart
  "**temps**: [[<side A temp>, ..], [<side B temp>, ..]]  
  count of temps in each side in **temps** should
  match **start-tube** and **end-tube**  
  **max-temp** & **min-temp** required."
  [{:keys [;; static
           max-temp min-temp
           start-tube end-tube
           design-temp target-temp
           side-names row-name
           ;; dynamic
           height width
           burner-on? avg-temp-band
           avg-raw-temp avg-temp temps
           raw?]}]
  (let [state (atom {})
        tooltip-state (r/atom nil)
        p-events {:mouseover (fn [_ d] (reset! tooltip-state d))
                  :mouseout (fn [_] (reset! tooltip-state nil))}
        color-1 (color-hex :royal-blue)
        color-2 (color-hex :teal)
        config {:node {:tag :g, :class :root
                       :nodes [(ch-defs-radial-fade "twt-grad-pt-shadow" :grad
                                                    (color-hex :alumina-grey)
                                                    0.3 1)
                               (ch-bands :h-bands-bg :h-bands-bg)
                               (ch-horizontal-lines :h-lines-bg :h-lines-bg)
                               (ch-bands :v-bands :v-bands)
                               (ch-bands :h-bands-avg :h-bands-avg)
                               (ch-horizontal-lines :h-lines :h-lines)
                               (ch-x-axis :x-axis :x-axis)
                               (ch-y-axis :y-axis :y-axis 25)
                               (ch-point-shadow :pt-shadow :pt-shadow
                                                "twt-grad-pt-shadow" 30)
                               (ch-marker-circle :points-a [:points 0] 6
                                                 color-1 p-events)
                               (ch-marker-square :points-b [:points 1] 6
                                                 color-2 p-events)
                               (twt-chart-legend color-1 color-2)]}}]

    (fn [{:keys [raw?] :as props}]
      (let [ks [:height :width
                :burner-on? :avg-temp-band
                :avg-raw-temp :avg-temp :temps]
            temp-unit @(rf/subscribe [::app-subs/temp-unit])
            y-title (str (if raw?
                           (translate [:twt-chart :y-axis :title-raw] "Raw TWT")
                           (translate [:twt-chart :y-axis :title-corrected]
                                      "Corrected TWT"))
                         " (" temp-unit ")")
            x-title (str row-name " - "
                         (translate [:twt-chart :x-axis :title] "Tube number")
                         " (" start-tube " - " end-tube ")")
            legend-labels
            {:side-names side-names
             :avg-temp
             (translate [:twt-chart :legend :avg-temp] "Avg temp")
             :avg-raw-temp
             (translate [:twt-chart :legend :avg-raw-temp] "Avg raw temp")
             :reduced-firing
             (translate [:twt-chart :legend :reduced-firing] "Reduced firing")
             :avg-temp-band
             (str "±" (if (= temp-unit deg-C) 20 36) temp-unit " "
                  (translate [:twt-chart :legend :avg-temp-band] "Avg temp"))}]
        ;; update chart data when required
        (when (not= (select-keys props ks)
                    (select-keys @state ks))
          (swap! state update-twt-data (assoc props :temp-unit temp-unit)))
        ;; draw the chart
        (let [tooltip @tooltip-state
              d3-config (twt-chart-d3-config @state config
                                             {:x-title x-title
                                              :y-title y-title
                                              :legend-labels legend-labels
                                              :tooltip tooltip})]
          [:div {:style {:user-select "none"
                         :position "relative"
                         :width width, :height height}}
           ;; chart
           [d3-svg d3-config]
           ;; hover toolitp
           (if tooltip [twt-popup {:state state, :tooltip-state tooltip-state}])
           ;; toolbar
           [:div {:style {:position "absolute"
                          :top 0, :right 0}}
            ;; export image
            [ic/camera {:style {:color (color-hex :sky-blue)
                                :margin "0 5px"
                                :cursor "pointer"}
                        :on-click #(twt-chart-export @state config
                                                     {:legend-labels legend-labels
                                                      :x-title x-title
                                                      :y-title y-title})}]]])))))
