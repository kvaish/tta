;; view elements component reformer-layout
(ns tta.component.reformer-layout.view
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.reformer-layout.style :as style]
            [tta.component.reformer-layout.subs :as subs]
            [tta.component.reformer-layout.event :as event]
            [ht.util.interop :as i]
            [cljsjs.d3]))

(defonce reformer-data
          (r/atom {
                   :configuration {
                                   :name         "Reformer",
                                   :version      4,
                                   :firing       "side",
                                   :dateModified "2018-01-04T04:49:22.949Z",
                                   :modifiedBy   "bhka@topsoe.com",
                                   :sfConfig     {
                                                  :chambers       [
                                                                   {
                                                                    :peepDoorCount     12,
                                                                    :sideNames         [
                                                                                        "A",
                                                                                        "B"
                                                                                        ],
                                                                    :sectionCount      4,
                                                                    :name              "Chamber A",
                                                                    :burnerCountPerRow 8,
                                                                    :endBurner         1,
                                                                    :peepDoorTubeCount [
                                                                                        2,
                                                                                        1,
                                                                                        2,
                                                                                        2,
                                                                                        1,
                                                                                        2,
                                                                                        2,
                                                                                        1,
                                                                                        2,
                                                                                        2,
                                                                                        1,
                                                                                        2
                                                                                        ],
                                                                    :tubeCount         20,
                                                                    :startBurner       8,
                                                                    :endTube           21,
                                                                    :startTube         40,
                                                                    :burnerRowCount    36
                                                                    },
                                                                   {
                                                                    :peepDoorCount     12,
                                                                    :sideNames         [
                                                                                        "C",
                                                                                        "D"
                                                                                        ],
                                                                    :sectionCount      4,
                                                                    :name              "Chamber B",
                                                                    :burnerCountPerRow 8,
                                                                    :endBurner         1,
                                                                    :peepDoorTubeCount [
                                                                                        2,
                                                                                        1,
                                                                                        2,
                                                                                        2,
                                                                                        1,
                                                                                        2,
                                                                                        2,
                                                                                        1,
                                                                                        2,
                                                                                        2,
                                                                                        1,
                                                                                        2
                                                                                        ],
                                                                    :tubeCount         20,
                                                                    :startBurner       8,
                                                                    :endTube           20,
                                                                    :startTube         1,
                                                                    :burnerRowCount    36
                                                                    },
                                                                   ],
                                                  :placementOfWHS "end",
                                                  :dualFuelNozzle true
                                                  }
                                   },
                   }))

(defn whs-position [ref]
  (let [whs-position (get-in ref [:configuration :sfConfig :placementOfWHS])
        attr (case whs-position
               "end" {:width 240 :height 50 :x 180 :y 50 :position "end"}
               "side" {:width 60 :height 200 :x 480 :y 170 :position "side"})]
    (assoc attr :text "WHS")))

(defn draw-whs [coll]
  (let [whs (i/ocall coll :merge coll)
        rects (i/ocall whs :select "rect.whs-box")
        texts (i/ocall whs :select "text.whs-text")]

    (-> rects
        (i/ocall :attr "fill" "lightgray")
        (i/ocall :attr "stroke" "black")
        (i/ocall :attr "height" (fn [d]
                                  (:height (whs-position d))))
        (i/ocall :attr "width" (fn [d] (:width (whs-position d))))
        (i/ocall :attr "x" (fn [d] (:x (whs-position d))))
        (i/ocall :attr "y" (fn [d] (:y (whs-position d)))))

    (-> texts
        (i/ocall :attr "stroke" "black")
        (i/ocall :attr "font-size" "45px")
        (i/ocall :attr "x" (fn [d]
                             (let [whs (whs-position d)]
                               (case (:position whs)
                                 "end" 250
                                 "side" 525))))
        (i/ocall :attr "y" (fn [d] (let [whs (whs-position d)]
                                     (case (:position whs)
                                       "end" 90
                                       "side" 320))))
        (i/ocall :attr "transform" (fn [d]
                                     (let [whs (whs-position d)]
                                       (case (:position whs)
                                         "end" ""
                                         "side" "rotate(270,525,320)"))))
        (i/ocall :text (fn [d] (:text (whs-position d)))))))

(defn draw-labels [ref-ch data]
  (js/console.log ref-ch data)
  (-> ref-ch
      (i/ocall :append "text")
      (i/ocall :attr "class" "chamber-name")
      (i/ocall :attr "font-size" "18px")
      (i/ocall :attr "x" (:x data))
      (i/ocall :attr "y" (:y data))
      (i/ocall :attr "transform" (str "rotate(" (:rotate data) "," (:x data) "," (:y data) ")"))
      (i/ocall :text (:text data))
      ))

(defn draw-chamber [coll]
  (let [news (i/ocall coll :enter)]
    ;; enter
    (let [chs (-> news
                  (i/ocall :append "g")
                  (i/ocall :attr "class" "chamber"))]
      (doto chs
        (->
          (i/ocall :append "rect"))
        (->
          (i/ocall :append "line"))
        (->
          (i/ocall :append "text")
          (i/ocall :attr "class" "chamber-name"))
        (->
          (i/ocall :append "text")
          (i/ocall :attr "class" "section"))
        (->
          (i/ocall :append "text")
          (i/ocall :attr "class" "side1-name"))
        (->
          (i/ocall :append "text")
          (i/ocall :attr "class" "side2-name"))
        (->
          (i/ocall :append "text")
          (i/ocall :attr "class" "side1-burner"))
        (->
          (i/ocall :append "text")
          (i/ocall :attr "class" "side2-burner"))
        (->
          (i/ocall :append "text")
          (i/ocall :attr "class" "tubes"))
        )

      ;; update
      (let [chs (i/ocall chs :merge coll)]
        (-> chs
            (i/ocall :select "rect")
            (i/ocall :attr "height" (fn [ch i]
                                      (:height ch)))
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "fill" "lightgray")
            (i/ocall :attr "width" (fn [ch i] (:width ch)))
            (i/ocall :attr "y" (fn [ch i] (:y ch)))
            (i/ocall :attr "x" (fn [ch i]
                                 (+ (:x ch) (* 300 (:index ch))))))

        (-> chs
            (i/ocall :select "line")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "stroke-width" "5px")
            (i/ocall :attr "x1" (fn [ch i] (+ (+ (:x ch) (* 300 (:index ch))) (/ (:width ch) 2))))
            (i/ocall :attr "y1" 150)
            (i/ocall :attr "x2" (fn [ch i] (+ (+ (:x ch) (* 300 (:index ch))) (/ (:width ch) 2))))
            (i/ocall :attr "y2" 400))

        #_(-> chs
            (i/ocall :selectAll "text.chamber-name")
            (i/ocall :each (fn [d i this]
                             (draw-labels (i/ocall js/d3 :select this)
                                          (assoc {:x 0 :y 0 :rotate 0} :text (get-in d [:data :name]))))))

        (-> chs
            (i/ocall :select "text.chamber-name")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "font-size" "14px")
            (i/ocall :attr "x" (fn [ch i] (case (:x ch)
                                            60 (+ (:x ch) (* 300 (:index ch)) (/ (:width ch) 4) 6)
                                            120 260)))
            (i/ocall :attr "y" 465)
            ;(i/ocall :attr "transform" (:rotate [90 350 30]))
            (i/ocall :text (fn [ch i]
                             (get-in ch [:data :name]))))

        (-> chs
            (i/ocall :select "text.section")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "font-size" "14px")
            (i/ocall :attr "x" (fn [ch i] (- (+ (:x ch) (* 300 (:index ch))) 6)))
            (i/ocall :attr "y" 180)
            (i/ocall :attr "transform" (fn [ch i] (str "rotate(270," (- (+ (:x ch) (* 300 (:index ch))) 6) ",180)")))
            (i/ocall :text (fn [ch i] (str (get-in ch [:data :sectionCount]) " Sections"))))

        (-> chs
            (i/ocall :select "text.tubes")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "font-size" "14px")
            (i/ocall :attr "x" (fn [ch i] (- (+ (:x ch) (* 300 (:index ch)) (/ (:width ch) 2)) 6)))
            (i/ocall :attr "y" (fn [ch i] (+ (:y ch) (/ (:height ch) 2) 50)))
            (i/ocall :attr "transform" (fn [ch i] (let [x (- (+ (:x ch) (* 300 (:index ch)) (/ (:width ch) 2)) 6)
                                                        y (+ (:y ch) (/ (:height ch) 2) 50)]
                                                    (str "rotate(270," x "," y ")"))))
            (i/ocall :text (fn [ch i] (str "Tubes " (get-in ch [:data :startTube]) "←" (get-in ch [:data :endTube])))))

        (-> chs
            (i/ocall :select "text.side1-name")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "font-size" "14px")
            (i/ocall :attr "x" (fn [ch i] (- (+ (:x ch) (* 300 (:index ch))) 6)))
            (i/ocall :attr "y" 450)
            (i/ocall :attr "transform" (fn [ch i] (let [x (- (+ (:x ch) (* 300 (:index ch))) 6)
                                                        y 450]
                                                    (str "rotate(270," x "," y ")"))))
            (i/ocall :text (fn [ch i] (get-in ch [:data :sideNames 0]))))

        (-> chs
            (i/ocall :select "text.side2-name")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "font-size" "14px")
            (i/ocall :attr "x" (fn [ch i] (+ (:x ch) (* 300 (:index ch)) (:width ch) 15)))
            (i/ocall :attr "y" 450)
            (i/ocall :attr "transform" (fn [ch i] (let [x (+ (:x ch) (* 300 (:index ch)) (:width ch) 15)
                                                        y 450]
                                                    (str "rotate(270," x "," y ")"))))
            (i/ocall :text (fn [ch i] (get-in ch [:data :sideNames 1]))))

        (-> chs
            (i/ocall :select "text.side1-burner")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "font-size" "14px")
            (i/ocall :attr "x" (fn [ch i] (+ (:x ch) (* 300 (:index ch)) 15)))
            (i/ocall :attr "y" 325)
            (i/ocall :attr "transform" (fn [ch i] (let [x (+ (:x ch) (* 300 (:index ch)) 15)
                                                        y 325]
                                                    (str "rotate(270," x "," y ")"))))
            (i/ocall :text (fn [ch i] (str "Burners " (get-in ch [:data :startBurner]) "←" (get-in ch [:data :endBurner])))))

        (-> chs
            (i/ocall :select "text.side2-burner")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "font-size" "14px")
            (i/ocall :attr "x" (fn [ch i] (- (+ (:x ch) (* 300 (:index ch)) (:width ch)) 6)))
            (i/ocall :attr "y" 325)
            (i/ocall :attr "transform" (fn [ch i] (let [x (- (+ (:x ch) (* 300 (:index ch)) (:width ch)) 6)
                                                        y 325]
                                                    (str "rotate(270," x "," y ")"))))
            (i/ocall :text (fn [ch i] (str "Burners " (get-in ch [:data :startBurner]) "←" (get-in ch [:data :endBurner])))))
        )))


  ;; exit
  (-> coll
      (i/ocall :exit)
      (i/ocall :remove)))

(defn draw-reformer [coll]
  (let [news (i/ocall coll :enter)]
    ;; enter
    (let [refs (-> news
                   (i/ocall :append "g")
                   (i/ocall :attr "class" "reformer"))
          whs (-> refs
                      (i/ocall :append "g")
                      (i/ocall :attr "class" "whs"))]
      (doto whs
        (->
          (i/ocall :append "rect")
          (i/ocall :attr "class" "whs-box"))
        (->
          (i/ocall :append "text")
          (i/ocall :attr "class" "whs-text")))
      (-> refs
          (i/ocall :append "g")
          (i/ocall :attr "class" "chambers"))

      ;; update
      (let [refs (i/ocall refs :merge coll)]
        (-> refs
            (i/ocall :select "g.whs")
            (draw-whs))

        (-> refs
            (i/ocall :select "g.chambers")
            (i/ocall :selectAll "g.chamber")
            (i/ocall :data (fn [r i]
                             (let [chs (get-in r [:configuration :sfConfig :chambers])
                                   attr (case (count chs)
                                          1 {:width 360 :height 350 :y 100 :x 120}
                                          2 {:width 180 :height 350 :y 100 :x 60})]
                               (to-array (map #(assoc attr :data %1 :index %2) chs (range))))))
            (draw-chamber))))

    ;; exit
    (-> coll
        (i/ocall :exit)
        (i/ocall :remove))))

(defn render-init [ele data]
  (-> js/d3
      (i/ocall :select ele)
      (i/ocall :selectAll "g.reformer")
      (i/ocall :data #js[data])
      #_(draw-reformer)))

(defn on-mount [this state]
  (let [ele (dom/dom-node this)
        data (:reformer-data (r/props this))]
    (swap! state assoc :container ele)
    (render-init ele data)))

(defn on-update [this state]
  (let [ele (:container @state)
        data (:reformer-data (r/props this))]
    (render-init ele data)))

(defn reformer-layout [props]
  (let [state (atom {})]
    (r/create-class
      {:reagent-render (fn [props]
                         (let [{:keys [width height]
                                :or   {width "100%" height "100%"}} props]
                           [:svg
                            {:view-box "0 0 600 500"
                             :style {:background-color "lightgray"
                                     :width width
                                     :height height}}]))
       :component-did-mount  (fn [this]
                               (on-mount this state))
       :component-did-update (fn [this _]
                               (on-update this state))
       })))
