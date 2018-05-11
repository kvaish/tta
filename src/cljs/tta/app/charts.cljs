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

(defn- abs [n] (if (pos? n) n (- n)))

(defn d3-scale [domain range]
  (let [scale (-> js/d3
                  (i/ocall :scaleLinear)
                  (i/ocall :domain (to-array domain))
                  (i/ocall :range (to-array range)))]
    scale))

(defn d3-ticks
  ([scale] (vec (i/ocall scale :ticks)))
  ([scale tick-count] (vec (i/ocall scale :ticks tick-count)))
  ([scale range min-gap] (d3-ticks scale range min-gap nil))
  ([scale range min-gap max-count]
   (let [n (js/Math.floor (/ (abs (apply - range)) min-gap))
         n (if (and (pos? max-count) (< max-count n)) max-count n)]
     (vec (i/ocall scale :ticks n)))))

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

(defn- data-x-axis [x-title x-scale x-ticks x-range y x-label-fn]
  {:title {:x (/ (apply + x-range) 2)
           :y (+ y 32)
           :text x-title}
   :ticks (map (fn [ti]
                 {:x (x-scale ti), :y (+ y 14)
                  :text (x-label-fn ti)})
               x-ticks)})

(defn- data-y-axis [y-title y-scale y-ticks y-range x y-label-fn]
  {:title {:x (- x 10)
           :y (/ (apply + y-range) 2)
           :text y-title}
   :ticks (->> (map (fn [ti]
                      {:x (+ x 10), :y (y-scale ti)
                       :text (y-label-fn ti)})
                    y-ticks)
               (filter (fn [{:keys [y]}]
                         (> (- y 14) (apply min y-range)))))})


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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn toolbar [state]
  (let [{:keys [on-export]} @state]
    [:div {:style {:position "absolute"
                   :top 0, :right 0}}
     ;; export image
     [ic/camera {:style {:color (color-hex :sky-blue)
                         :margin "0 5px"
                         :cursor "pointer"}
                 :on-click #(on-export state)}]]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; Layouts ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ch-background [color]
  {:tag :rect, :class :background
   :attr {:x 0, :y 0, :width "100%", :height "100%"
          :fill color}})

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
                   :patternTransform "rotate(45)"
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
(defn- ch-rects [class-kw data-key events]
  {:tag :rect, :class class-kw
   :attr {:x :x, :y :y
          :height :h, :width :w
          :stroke :color, :fill :color}
   :multi? true
   :data data-key
   :on events})

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
  (let [attr-h {:style (str "font-size:12px;font-weight:700;
                             fill:" (color-hex :bitumen-grey))
                :text-anchor "middle"
                :x :x, :y :y}
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


;; Shaded areas
(defn- ch-diagonal-shading [class-kw data-key fill-id color]
  {:tag :rect, :class class-kw
   :data data-key
   :multi? true
   :attr {:x :x :y :y :height :h :width :w
          :style (str "stroke-width:1;stroke:" color)
          :fill (str "url(#" fill-id ")")}})

;; Chamber box
(defn- ch-box [class-kw data-key]
  {:tag :rect, :class class-kw
   :data data-key
   :attr {:x :x, :y :y, :height :h, :width :w
          :stroke (color-hex :bitumen-grey)
          :stroke-width 4
          :fill "none", :rx 6, :ry 6}})


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; OVERALL-TWT-CHART;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn- overall-twt-tube-markers []
  (let [f (fn [kw os] #(+ (kw %) os))]
    {:tag :g, :class :tube-markers
     :data :tube-markers, :multi? true
     :nodes [{:tag :circle, :class :marker
              :attr {:cx :x, :cy :y, :r 3
                     :fill (color-hex :alumina-grey -20)}}
             {:tag :text, :class :label
              :text :text
              :attr {:x (f :x 10) :y (f :y 3)
                     :style (str "font-size:10px;fill:"
                                 (color-hex :bitumen-grey))}}]}))

(defn- overall-twt-burner-markers [fill-id color events]
  {:tag :g, :class :reduced-burners
   :data :reduced-burners, :multi? true
   :nodes [{:tag :rect, :class :box, :data :box
            :attr {:x :x :y :y :height :h :width :w
                   :stroke color, :fill (str "url(#" fill-id ")")}}
           {:tag :circle, :class :markers
            :data :markers, :multi? true
            :attr {:cx :x, :cy :y, :r 6, :fill color}
            :on events}]})

(defn- overall-twt-tubes-lines []
  {:tag :line, :class :tubes-lines
   :data :tubes-lines, :multi? true
   :attr {:x1 :x, :y1 :y1, :x2 :x, :y2 :y2
          :stroke-dasharray :dasharray
          :stroke-width 2
          :stroke (color-hex :bitumen-grey)}})

(defn overall-twt-chart-title []
  (let [f (fn [kw os] #(+ (kw %) os))]
    {:tag :g, :class :chart-title
     :data :chart-title
     :nodes [{:tag :text, :class :title
              :attr {:x :x, :y (f :y 16)
                     :style (str "font-size:12px;font-weight:700;fill:"
                                 (color-hex :bitumen-grey))}
              :text [:text :title]}
             {:tag :text, :class :sub-title
              :attr {:x :x, :y (f :y 32)
                     :style (str "font-size:10px;fill:" (color-hex :bitumen-grey))}
              :text [:text :sub-title]}]}))

(def overall-twt-colors [[0 (color-hex :yellow)]
                         [100 (color-hex :red)]])

(defn- overall-twt-legend-scale []
  (let [stops (mapv (fn [i [p c]]
                      {:tag :stop, :class (keyword (str "pct" i))
                       :attr {:offset (str p "%")
                              :style (str "stop-opacity:1;stop-color:" c)}})
                    (range) overall-twt-colors)]
    {:tag :g, :class :legend-scale, :data :legend-scale
     :nodes [{:tag :defs, :class :defs
              :nodes [{:tag :linearGradient, :class :grad
                       :attr {:id "scale-grad"
                              :x1 "0%", :x2 "100%", :y1 "0%", :y2 "0%"}
                       :nodes stops}]}
             {:tag :rect, :class :scale
              :data :scale
              :attr {:x :x, :y :y, :width :w, :height :h, :fill "url(#scale-grad)"}}
             {:tag :line, :class :ticks
              :data :ticks, :multi? true
              :attr {:x1 :x, :y1 :y1, :x2 :x, :y2 :y2
                     :stroke (color-hex :bitumen-grey)}}
             {:tag :text, :class :labels
              :data :labels, :multi? true
              :text :text
              :attr {:x :x, :y :y
                     :style (str "font-size:10px;fill:" (color-hex :bitumen-grey))
                     :text-anchor "middle"}}]}))

(defn update-overall-twt-data [state props]
  (let [{:keys [wall-names tube-rows burner-rows max-temp min-temp
                height width burner-on? temps]} props
        {:keys [temp-unit]} state

        x-ps-os 10, x-as-os 10, x-ae-os 10, x-pe-os 10
        y-ps-os 35, y-pe-os 90
        x-ts 16, x-ws 20, x-bs 32
        lg-count 3, lg-i-w 100

        px-start x-ps-os, px-end (- width x-pe-os), px-width (- px-end px-start)
        ax-start (+ px-start x-as-os)
        ax-end (- px-end x-ae-os)
        ax-width (- ax-end ax-start)
        y-start y-ps-os, y-end (- height y-pe-os), y-height (- y-end y-start)

        ;; y-axis (tube-number)
        t-row-count (count tube-rows)
        t-count (let [{:keys [start-tube end-tube]} (first tube-rows)]
                  (inc (abs (- end-tube start-tube))))
        y-domain [-0.5 (- t-count 0.5)]
        y-range [y-start y-end]
        y-scale (d3-scale y-domain y-range)
        y-ticks (d3-ticks y-scale y-range 30 t-count)

        ;; burner scale (y-axis)
        b-row-count (count burner-rows)
        b-count (let [{:keys [start-burner end-burner]} (first burner-rows)]
                  (inc (abs (- end-burner start-burner))))
        bf? (< t-row-count b-row-count) ;; burner first?
        b-domain [-0.5 (- b-count 0.5)]
        b-range [y-start y-end]
        b-scale (d3-scale b-domain b-range)

        t-label-fn (fn [row i]
                     (let [{:keys [start-tube end-tube]} (get tube-rows row)]
                       ((if (> end-tube start-tube) + -) start-tube i)))
        b-label-fn (fn [row i]
                     (let [{:keys [start-burner end-burner]} (get burner-rows row)]
                       ((if (> end-burner start-burner) + =) start-burner i)))
        tr-label-fn #(get-in tube-rows [% :name])
        temp-label-fn #(str (js/Math.round (to-temp-unit % temp-unit)) " " temp-unit)
        side-label-fn (fn [side] (if (zero? side) (:west wall-names) (:east wall-names)))

        ;; legend data
        lg-w (* lg-count lg-i-w), lg-x (- px-end lg-w)
        [max-temp min-temp lg-temps lg-x]
        (let [tmax (* 10 (inc (quot (to-temp-unit max-temp temp-unit) 10)))
              tmin (to-temp-unit min-temp temp-unit)
              dt (* 10 (inc (quot (/ (- tmax tmin) lg-count) 10)))
              tmin (- tmax (* dt lg-count))]
          [(from-temp-unit tmax temp-unit)
           (from-temp-unit tmin temp-unit)
           (mapv #(+ tmin (* % dt)) (range (inc lg-count)))
           (mapv #(+ lg-x (* % lg-i-w)) (range (inc lg-count)))])

        legend-scale (let [y (- height 24), half-w (/ lg-i-w 2)
                           y1 (- y 20), y2 (+ y 20)]
                       {:ticks (map (fn [x] {:x x, :y1 y1, :y2 y2}) lg-x)
                        :labels (map (fn [x t0 t1]
                                       {:text (str t0 " - " t1 " " temp-unit)
                                        :x (+ x half-w), :y (+ y1 14)})
                                     lg-x lg-temps (rest lg-temps))
                        :scale {:x (first lg-x), :y y, :w lg-w, :h 20}})

        ;; temp scale
        temp-domain [min-temp max-temp]
        temp-scale (d3-scale temp-domain (map second overall-twt-colors))

        ;; x-axis: burner, burner marker rect, tube, temp marker rect
        tw (/ (- ax-width x-ws x-ws (* x-bs b-row-count)) t-row-count)
        mw (/ (- tw x-ts) 2), mh (/ y-height t-count)
        x-start (+ ax-start x-ws), x-end (- ax-end x-ws)
        t-x (map #(+ x-start (/ tw 2) (if bf? x-bs 0) (* (+ x-bs tw) %))
                 (range t-row-count))
        temp-x-l (map #(- % (/ tw 2)) t-x)
        temp-x-r (map #(+ % mw x-ts) temp-x-l)
        b-x (map #(+ x-start (/ x-bs 2) (* (+ x-bs tw) %) (if bf? 0 tw))
                 (range b-row-count))
        bm-xw (let [bt (/ mw 3)]
                (concat (if bf? [[x-start (+ bt x-bs)]])
                        (map #(list (- (+ mw %) bt) (+ bt bt x-bs)) (butlast temp-x-r))
                        (if bf? [[(- x-end bt x-bs) (+ bt x-bs)]])))

        x-axis {:ticks (mapv (fn [x tr]
                               {:x x, :y (+ y-end 24)
                                :text (:name tr)})
                             t-x tube-rows)}

        tube-markers (mapcat (fn [x tri]
                               (map (fn [ti]
                                      {:x x, :y (y-scale ti)
                                       :text (t-label-fn tri ti)})
                                    y-ticks))
                            t-x (range))

        tubes-lines (let [gap (- (y-scale 1) 2 (y-scale 0))
                          dasharray (if (pos? gap) (str "2," gap))]
                      (map (fn [x] {:y1 (dec (y-scale 0))
                                   :y2 (y-scale (- t-count 0.5))
                                   :x x, :dasharray dasharray})
                           t-x))

        temp-markers (mapcat (fn [temps tri xl xr]
                               (mapcat (fn [temps si x]
                                         (->> (map (fn [temp ti]
                                                     {:x x, :y (y-scale (- ti 0.5))
                                                      :w mw, :h mh
                                                      :color (if (pos? temp)
                                                               (temp-scale temp))
                                                      :tri tri, :si si, :ti ti
                                                      :temp temp})
                                                   temps (range))
                                              (filter :color)))
                                       temps (range) [xl xr]))
                             temps (range) temp-x-l temp-x-r)

        reduced-burners (mapcat (fn [bri burner-on? bx [bmx bmw]]
                                  (map (fn [[bi be]]
                                         (let [bip (b-scale bi)
                                               bep (b-scale be)
                                               bs (range (js/Math.ceil bi) be 1)]
                                           {:box {:x bmx, :y bip
                                                  :w bmw, :h (- bep bip)}
                                            :markers (map (fn [bi]
                                                            {:x bx, :y (b-scale bi)
                                                             :bri bri, :bi bi})
                                                          bs)}))
                                       (burner-bands burner-on?)))
                                (range) burner-on? b-x bm-xw)

        chamber-box {:x (+ ax-start x-ws -10), :y (- y-start 10)
                     :w (- ax-width x-ws x-ws -20), :h (+ y-height 20)}

        x-l (+ ax-start x-ws -16), x-r (+ ax-end 4)
        y-mid (/ (+ y-start y-end) 2)
        ;; x-mid (- (first (drop (quot t-row-count 2) t-x)) mw (/ x-bs 2))
        x-mid (/ (+ ax-start ax-end) 2)
        wall-name-pos {:east {:x x-r, :y y-mid}
                       :west {:x x-l, :y y-mid}
                       :north {:x x-mid, :y (- y-start 16)}
                       :south {:x x-mid, :y (+ y-end 42)}}
        wall-names (reduce-kv (fn [m k v]
                                (assoc m k (assoc (get wall-name-pos k) :text v)))
                              {} wall-names)
        chart-title {:x px-start, :y (- height 48)}]

    (assoc state
           :props props
           :data {:x-axis x-axis
                  :tube-markers tube-markers
                  :tubes-lines tubes-lines
                  :temp-markers temp-markers
                  :wall-names wall-names
                  :chamber-box chamber-box
                  :reduced-burners reduced-burners
                  :legend-scale legend-scale
                  :chart-title chart-title}
           :t-label-fn t-label-fn
           :b-label-fn b-label-fn
           :tr-label-fn tr-label-fn
           :temp-label-fn temp-label-fn
           :side-label-fn side-label-fn)))

(defn overall-twt-chart-d3-config [state export? options]
  (let [{:keys [config data props]} state
        {:keys [width height title sub-title]} props
        data (cond->
                 (assoc-in data [:chart-title :text] (if export?
                                                       {:title title
                                                        :sub-title sub-title}))
               (not (:reduced-firing? options)) (assoc :reduced-burners []))]
    (assoc config :data data
           :view-box (str "0 0 " width " " height)
           :width width, :height height)))

(defn overall-twt-chart-export [state]
  (let [width 1000, height 700
        {:keys [props] :as state} @state
        state (update-overall-twt-data state
                                       (assoc props :width width, :height height))
        d3-config (overall-twt-chart-d3-config state true
                                               {:reduced-firing? true})
        svg-string (d3-svg->string d3-config)]
    (save-svg-to-file "overall-twt-graph.png" svg-string width height 30)))

(defn overall-twt-popup-temp [data state]
  (let [tw 80, th 50
        {:keys [t-label-fn tr-label-fn
                temp-label-fn side-label-fn]} @state
        {:keys [x y w h tri si ti temp color]} data
        {:keys [x y]} (tooltip-pos {:tw tw, :th th
                                    :ax x, :ay y, :aw w, :ah h
                                    :adx (+ w 5), :tdx 0, :tym 5})]
    [:div {:style {:width tw, :height th
                   :padding 0
                   :background (color-hex :white)
                   :border (str "1px solid " (color-hex :bitumen-grey))
                   :border-radius "6px"
                   :position "absolute"
                   :left x, :top y}}
     [:svg {:view-box (str "0 0 " tw " " th)
            :width tw, :height th
            :style {:font-size "10px"
                    :margin 0
                    :color (color-hex :bitumen-grey)}}
      [:rect {:x 10, :y 5, :width (- tw 20), :height 14, :fill color}]
      [:text {:x (/ tw 2), :y 16, :text-anchor "middle"
              :font-weight 700, :fill (color-hex :royal-blue)}
       (temp-label-fn temp)]
      [:text {:x 10, :y 30, :fill "currentColor"}
       (str (t-label-fn tri ti) ", " (side-label-fn si))]
      [:text {:x 10, :y 42, :fill "currentColor"} (tr-label-fn tri)]]]))

(defn overall-twt-popup-burner [data state]
  (let [tw 30, th 30
        {:keys [b-label-fn]} @state
        {:keys [x y bri bi]} data
        {:keys [x y]} (tooltip-pos {:tw tw, :th th
                                    :ax (- x 6), :ay (- y 6), :aw 12, :ah 12
                                    :adx 15, :tdx 0, :tym 3})]
    [:div {:style {:width tw, :height th
                   :padding "6px"
                   :background (color-hex :white)
                   :border (str "1px solid " (color-hex :bitumen-grey))
                   :border-radius "50%"
                   :position "absolute"
                   :left x, :top y
                   :text-align "center"
                   :font-size "12px", :font-weight 700
                   :color (color-hex :bitumen-grey)}}
     (b-label-fn bri bi)]))

(defn overall-twt-popup [state]
  (let [{:keys [tooltip]} @state
        {:keys [data ttype]} @tooltip]
    (case ttype
      :burner [overall-twt-popup-burner data state]
      :temp [overall-twt-popup-temp data state]
      nil)))

(defn overall-twt-chart
  "**temps**: [[[<side A temp>, ..], [<side B temp>, ..]], ..]  
  **burner-on?**: [[true/false, ..]]  
  **max-temp** & **min-temp** required."
  [{:keys [;; static
           wall-names tube-rows burner-rows max-temp min-temp title sub-title
           ;; dynamic
           width height burner-on? temps]}
   {:keys [reduced-firing?]}]
  (let [state (atom {})
        tooltip (r/atom nil)
        t-events {:mouseover (fn [_ d] (reset! tooltip {:ttype :temp, :data d}))
                  :mouseout (fn [_] (reset! tooltip nil))}
        b-events (assoc t-events :mouseover
                        (fn [_ d] (reset! tooltip {:ttype :burner, :data d})))
        b-color (color-hex :sky-blue)
        config {:node {:tag :g, :class :root
                       :nodes [(ch-background (color-hex :white))
                               (ch-defs-diagonal-lines "shade" :shade b-color 8)
                               (ch-x-axis :x-axis :x-axis)
                               (ch-rects :temp-markers :temp-markers t-events)
                               (ch-walls :wall-names :wall-names)
                               (ch-box :chamber-box :chamber-box)
                               (overall-twt-burner-markers "shade" b-color b-events)
                               (overall-twt-tube-markers)
                               (overall-twt-tubes-lines)
                               (overall-twt-legend-scale)
                               (overall-twt-chart-title)]}}]
    (swap! state assoc :config config, :tooltip tooltip
           :on-export overall-twt-chart-export)

    (fn [{:keys [width height] :as props} options]
      (let [ks [:height :width :burner-on? :temps]
            temp-unit @(rf/subscribe [::app-subs/temp-unit])]
        (swap! state assoc :temp-unit temp-unit)
        ;; update chart data when required
        (when (not= (select-keys props ks)
                    (select-keys (:props @state) ks))
          (swap! state update-overall-twt-data props))
        ;; draw the chart
        (let [d3-config (overall-twt-chart-d3-config @state false options)]
          [:div {:style {:user-select "none"
                         :position "relative"
                         :width width, :height height}}
           ;; chart
           [d3-svg d3-config]
           ;; hover tooltip
           [overall-twt-popup state]
           ;; toolbar
           [toolbar state]])))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TWT-CHART ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
                              :x (f :x 100), :y (f :y 3)
                              :fill color-2}}
                      {:tag :text, :class :side-right
                       :attr {:style label-style
                              :x (f :x 118), :y (f :y 9)}
                       :text [:labels :side-names 1]}]}
             {:tag :g, :class :avg-temp
              :nodes [{:tag :line, :class :line
                       :attr {:x1 (f :x 250), :x2 (f :x 270)
                              :y1 (f :y 6), :y2 (f :y 6)
                              :stroke (color-hex :ocean-blue)
                              :shape-rendering "crispEdges"}}
                      {:tag :text, :class :label
                       :attr {:style label-style
                              :x (f :x 275), :y (f :y 9)}
                       :text [:labels :avg-temp]}]}
             {:tag :g, :class :avg-raw-temp
              :nodes [{:tag :line, :class :line
                       :attr {:x1 (f :x 350), :x2 (f :x 370)
                              :y1 (f :y 6), :y2 (f :y 6)
                              :stroke (color-hex :ocean-blue)
                              :shape-rendering "crispEdges"
                              :stroke-dasharray "5,5"}}
                      {:tag :text, :class :label
                       :attr {:style label-style
                              :x (f :x 375), :y (f :y 9)}
                       :text [:labels :avg-raw-temp]}]}
             {:tag :g, :class :reduced-firing
              :nodes [{:tag :rect, :class :rect
                       :attr {:x (f :x 450), :y :y, :width 20, :height 12
                              :fill (color-rgba :red 30 0.5)}}
                      {:tag :text, :class :label
                       :attr {:style label-style
                              :x (f :x 475), :y (f :y 9)}
                       :text [:labels :reduced-firing]}]}
             {:tag :g, :class :avg-temp-band
              :nodes [{:tag :rect, :class :rect
                       :attr {:x (f :x 550), :y :y, :width 20, :height 12
                              :fill (color-rgba :ocean-blue 50 0.5)}}
                      {:tag :text, :class :label
                       :attr {:style label-style
                              :x (f :x 575), :y (f :y 9)}
                       :text [:labels :avg-temp-band]}]}]}))

(defn twt-chart-title []
  (let [f (fn [kw os] #(+ (kw %) os))]
    {:tag :g, :class :chart-title
     :data :chart-title
     :nodes [{:tag :text, :class :title
              :attr {:x :x, :y (f :y 16)
                     :style (str "font-size:12px;font-weight:700;fill:"
                                 (color-hex :bitumen-grey))
                     :text-anchor "middle"}
              :text [:text :title]}
             {:tag :text, :class :sub-title
              :attr {:x :x, :y (f :y 32)
                     :style (str "font-size:10px;fill:" (color-hex :bitumen-grey))
                     :text-anchor "middle"}
              :text [:text :sub-title]}]}))

(defn update-twt-data [state props]
  (let [state (merge state props)
        {:keys [max-temp min-temp design-temp target-temp
                start-tube end-tube
                height width temps
                burner-on? avg-temp-band avg-raw-temp avg-temp]} props
        {:keys [temp-unit]} state

        x-ps-os 30, x-pe-os 5, x-as-os 40
        y-ps-os 40, y-pe-os 24
        plot-x-start x-ps-os
        plot-x-width (- width x-ps-os x-pe-os)
        plot-x-end (- width x-pe-os)

        tube-count (inc (abs (- end-tube start-tube)))
        burner-count (count burner-on?)

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
        x-ticks (filter #(< -1 % tube-count) (d3-ticks x-scale x-range 50 tube-count))
        y-ticks (->> (d3-ticks (as-> (map #(to-temp-unit % temp-unit) y-domain) $
                                 (d3-scale $ y-range))
                               y-range 30)
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
                       false tlo]]
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
        avg-temp-bands (let [[from to] avg-temp-band
                             from-p (y-scale from)
                             to-p (y-scale to)]
                         [{:x x-start
                           :y (min from-p to-p)
                           :w x-width
                           :h (abs (- from-p to-p))
                           :color (color-rgba :ocean-blue 50 0.5)}])

        ;; reduced firing bands
        reduced-firing-bands (mapv (fn [[bi be]]
                                     (let [bip (b-scale bi)
                                           bep (b-scale be)]
                                       {:x bip, :y y-start
                                        :w (- bep bip)
                                        :h y-height
                                        :color (color-rgba :red 30 0.5)}))
                                   (burner-bands burner-on?))

        ;; avg temp lines
        avg-raw-temp-lines (if-not (pos? avg-raw-temp) []
                                   [{:line {:x1 x-start, :x2 x-end
                                            :y (y-scale avg-raw-temp),
                                            :color (color-hex :ocean-blue)
                                            :dashed? true}}])
        avg-temp-lines (if-not (pos? avg-temp) []
                               [{:line {:x1 x-start, :x2 x-end
                                        :y (y-scale avg-temp)
                                        :color (color-hex :ocean-blue)}}])

        ;; legend position
        legend  {:x plot-x-start, :y (- (/ y-start 2) 6)}
        ;; chart title position
        chart-title {:x (+ x-start (/ x-width 2)), :y y-start}]

    (assoc state
           :props props
           :data {:points points
                  :y-axis y-axis
                  :x-axis x-axis
                  :h-lines h-lines
                  :h-lines-bg h-lines-bg
                  :h-bands-bg h-bands-bg
                  :reduced-firing-bands reduced-firing-bands
                  :avg-temp-bands avg-temp-bands
                  :avg-raw-temp-lines avg-raw-temp-lines
                  :avg-temp-lines avg-temp-lines
                  :legend legend
                  :chart-title chart-title}
           :x-label-fn x-label-fn
           :y-label-fn y-label-fn)))

(defn twt-popup-temp [data state]
  (let [{:keys [x-label-fn y-label-fn temp-unit]
         {:keys [side-names]} :props} @state
        {:keys [x y si ti t]} data
        w 80, h 50
        {:keys [x y]} (tooltip-pos {:tw w, :th h
                                    :ax (- x 3), :ay (- y 3)
                                    :aw 6, :ah 6
                                    :adx 11, :tdx 0, :tym 5})]
    (if data
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
         (str (y-label-fn t) " " temp-unit)]]])))

(defn twt-popup [state]
  (let [{:keys [tooltip]} @state
        {:keys [data ttype]} @tooltip]
    (case ttype
      :temp [twt-popup-temp data state]
      nil)))

(defn twt-chart-d3-config [state export? options]
  (let [{:keys [config data props
                x-title y-title legend-labels]} state
        {:keys [width height title sub-title]} props
        data (-> data
                 (assoc-in [:y-axis :title :text] y-title)
                 (assoc-in [:x-axis :title :text] x-title)
                 (assoc-in [:legend :labels] legend-labels)
                 (assoc-in [:chart-title :text] (if export?
                                                  {:title title
                                                   :sub-title sub-title})))
        data (cond-> data
               (not (:reduced-firing? options)) (assoc :reduced-firing-bands [])
               (not (:avg-temp-band? options)) (assoc :avg-temp-bands [])
               (not (:avg-raw-temp? options)) (assoc :avg-raw-temp-lines [])
               (not (:avg-temp? options)) (assoc :avg-temp-lines []))]
    (assoc config :data data
           :view-box (str "0 0 " width " " height)
           :width width, :height height)))

(defn twt-chart-export [state]
  (let [ width 700, height 500
        {:keys [props] :as state} @state
        state (update-twt-data state (assoc props :width width, :height height))
        d3-config (twt-chart-d3-config state true
                                       {:reduced-firing? true
                                        :avg-temp-band? true
                                        :avg-raw-temp? true
                                        :avg-temp? true})
        svg-string (d3-svg->string d3-config)]
    (save-svg-to-file "chart.png" svg-string width height 30)))

(defn twt-chart
  "**temps**: [[<side A temp>, ..], [<side B temp>, ..]]  
  **burner-on?**: [true/false, ..]  
  **max-temp** & **min-temp** required."
  [{:keys [;; static
           side-names row-name max-temp min-temp
           start-tube end-tube design-temp target-temp
           title sub-title
           ;; dynamic
           height width
           burner-on? avg-temp-band
           avg-raw-temp avg-temp temps
           raw?]}
   {:keys [reduced-firing? avg-temp-band? avg-temp? avg-raw-temp?]}]
  (let [state (atom {})
        tooltip (r/atom nil)
        p-events {:mouseover (fn [_ d] (reset! tooltip {:ttype :temp, :data d}))
                  :mouseout (fn [_] (reset! tooltip nil))}
        color-1 (color-hex :royal-blue)
        color-2 (color-hex :teal)
        config {:node {:tag :g, :class :root
                       :nodes [(ch-background (color-hex :white))
                               (ch-bands :h-bands-bg :h-bands-bg)
                               (ch-horizontal-lines :h-lines-bg :h-lines-bg)
                               (ch-bands :firing :reduced-firing-bands)
                               (ch-bands :atb :avg-temp-bands)
                               (ch-horizontal-lines :h-lines :h-lines)
                               (ch-horizontal-lines :atl :avg-temp-lines)
                               (ch-horizontal-lines :artl :avg-raw-temp-lines)
                               (ch-x-axis :x-axis :x-axis)
                               (ch-y-axis :y-axis :y-axis 25)
                               (ch-marker-circle :points-a [:points 0] 6
                                                 color-1 p-events)
                               (ch-marker-square :points-b [:points 1] 6
                                                 color-2 p-events)
                               (twt-chart-legend color-1 color-2)
                               (twt-chart-title)]}}]
    (swap! state assoc :config config, :tooltip tooltip
           :on-export twt-chart-export)

    (fn [{:keys [raw? width height] :as props} options]
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
             (translate [:twt-chart :legend :avg-temp-band] "{delta} off Avg temp"
                        {:delta (str "±" (if (= temp-unit deg-C) 20 36)
                                     " " temp-unit)})}]
        (swap! state assoc :temp-unit temp-unit
               :x-title x-title, :y-title y-title
               :legend-labels legend-labels)
        ;; update chart data when required
        (when (not= (select-keys props ks)
                    (select-keys (:props @state) ks))
          (swap! state update-twt-data props))
        ;; draw the chart
        (let [d3-config (twt-chart-d3-config @state false options)]
          [:div {:style {:user-select "none"
                         :position "relative"
                         :width width, :height height}}
           ;; chart
           [d3-svg d3-config]
           ;; hover tooltip
           [twt-popup state]
           ;; toolbar
           [toolbar state]])))))
