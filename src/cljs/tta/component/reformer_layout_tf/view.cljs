;; view elements component reformer-layout-tf
(ns tta.component.reformer-layout-tf.view
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
            [tta.component.reformer-layout-tf.style :as style]
            [tta.component.reformer-layout-tf.subs :as subs]
            [tta.component.reformer-layout-tf.event :as event]
            [ht.util.interop :as i]
            [cljsjs.d3]))

(defonce reformer-data-tf
         (r/atom {
                  :configuration {
                                  :name         "Reformer",
                                  :version      4,
                                  :firing       "top",
                                  :date-modified "2018-01-04T04:49:22.949Z",
                                  :modified-by   "bhka@topsoe.com",
                                  :tf-config  {
                                               :wall-names {
                                                            :north "North"
                                                            :east "East"
                                                            :west "West"
                                                            :south "South"
                                                            },
                                               :burner-first? true,
                                               :tube-row-count 4,
                                               :tube-rows [
                                                           {
                                                            :tube-count 10
                                                            :start-tube 1
                                                            :end-tube 10
                                                            },
                                                           {
                                                            :tube-count 10
                                                            :start-tube 1
                                                            :end-tube 10
                                                            },
                                                           {
                                                            :tube-count 10
                                                            :start-tube 1
                                                            :end-tube 10
                                                            },
                                                           {
                                                            :tube-count 10
                                                            :start-tube 1
                                                            :end-tube 10
                                                            },
                                                           ],
                                               :burner-row-count 5,
                                               :burner-rows [
                                                             {
                                                              :burner-count 5
                                                              :start-burner 1
                                                              :end-burner 5
                                                              },
                                                             {
                                                              :burner-count 5
                                                              :start-burner 1
                                                              :end-burner 5
                                                              },
                                                             {
                                                              :burner-count 5
                                                              :start-burner 1
                                                              :end-burner 5
                                                              },
                                                             {
                                                              :burner-count 5
                                                              :start-burner 1
                                                              :end-burner 5
                                                              },
                                                             {
                                                              :burner-count 5
                                                              :start-burner 1
                                                              :end-burner 5
                                                              }],
                                               :section-count 2,
                                               :sections [
                                                          {
                                                           :tube-count 5
                                                           :burner-count 3
                                                           },
                                                          {
                                                           :tube-count 5
                                                           :burner-count 2
                                                           }],
                                               :measure-levels {
                                                                :top? true
                                                                :middle? true
                                                                :bottom? true
                                                                }
                                               }
                                  },
                  }))

(defn draw-tubes [coll]
  (let [news (i/ocall coll :enter)]
    ;;enter
    (let [refs (-> news
                   (i/ocall :append "g")
                   (i/ocall :attr "class" "tube-row"))]
      (-> news
          (i/ocall :append "line"))

      ;;update
      (let [tube-rows (i/ocall news :merge coll)]
        (-> tube-rows
            (i/ocall :selectAll "line")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "stroke-width" "8px")
            (i/ocall :attr "x1" (fn [d i] (:x1 d)))
            (i/ocall :attr "y1" (fn [d i] (:y1 d)))
            (i/ocall :attr "x2" (fn [d i] (:x2 d)))
            (i/ocall :attr "y2" (fn [d i] (:y2 d)))))))
  ;;exit
  (-> coll
      (i/ocall :exit)
      (i/ocall :remove)))

(defn draw-label [coll]
  (let [news (i/ocall coll :enter)]
    ;;enter
    (let [refs (-> news
                   (i/ocall :append "g")
                   (i/ocall :attr "class" (fn [d i] (:class d))))]
      (-> refs
          (i/ocall :append "text"))

      ;;update
      (let [tube-rows (i/ocall refs :merge coll)]
        (-> tube-rows
            (i/ocall :select "text")
            (i/ocall :attr "x" (fn [d i] (:x d)))
            (i/ocall :attr "y" (fn [d i] (:y d)))
            (i/ocall :text (fn [d i] (:text d)))
            (i/ocall :attr "transform" (fn [d i] (:rotate d)))))))
  ;;exit
  (-> coll
      (i/ocall :exit)
      (i/ocall :remove)))

(defn draw-burners [coll]
  (let [news (i/ocall coll :enter)]
    ;;enter
    (let [refs (-> news
                   (i/ocall :append "g")
                   (i/ocall :attr "class" "burner-row"))]
      (-> refs
          (i/ocall :append "line"))
      ;;update
      (let [tube-rows (i/ocall refs :merge coll)]
        (-> tube-rows
            (i/ocall :select "line")
            (i/ocall :attr "stroke" "red")
            (i/ocall :attr "stroke-width" "3px")
            (i/ocall :attr "stroke-dasharray" "8,8")
            (i/ocall :attr "x1" (fn [d i] (:x1 d)))
            (i/ocall :attr "y1" (fn [d i] (:y1 d)))
            (i/ocall :attr "x2" (fn [d i] (:x2 d)))
            (i/ocall :attr "y2" (fn [d i] (:y2 d)))))))
  ;;exit
  (-> coll
      (i/ocall :exit)
      (i/ocall :remove)))

(defn draw-walls [ref]
  (let [news (i/ocall ref :enter)]
    ;;enter
    (let [walls (-> news
                    (i/ocall :append "g")
                    (i/ocall :attr "class" "side"))]
      (-> walls
          (i/ocall :append "text")
          (i/ocall :attr "class" "side-name"))

      ;;update
      (let [walls (i/ocall walls :merge ref)]
        (-> walls
            (i/ocall :select "text.side-name")
            (i/ocall :attr "x" (fn [d i] (:x d)))
            (i/ocall :attr "y" (fn [d i] (:y d)))
            (i/ocall :text (fn [d i] (:text d)))
            (i/ocall :attr "transform" (fn [d i] (str "rotate(" (:rotate d) ")"))))))

    ;;exit
    (-> ref
        (i/ocall :exit)
        (i/ocall :remove))))

(defn draw-chamber [coll]
  (let [news (i/ocall coll :enter)]
    ;;enter
    (let [refs (-> news
                   (i/ocall :append "g")
                   (i/ocall :attr "class" "container"))
          box (-> refs
                  (i/ocall :append "g")
                  (i/ocall :attr "class" "box"))]
      (-> box
          (i/ocall :append "rect"))
      (-> refs
          (i/ocall :append "g")
          (i/ocall :attr "class" "tube-rows"))
      (-> refs
          (i/ocall :append "g")
          (i/ocall :attr "class" "burner-rows"))
      ;;update
      (let [refs (i/ocall news :merge coll)]
        (-> refs
            (i/ocall :select "rect")
            (i/ocall :attr "stroke" "black")
            (i/ocall :attr "fill" "lightgray")
            (i/ocall :attr "height" 400)
            (i/ocall :attr "width" (fn [d] (+ (get-in d [:configuration :dimen :width]) 50)))
            (i/ocall :attr "y" 50)
            (i/ocall :attr "x" 50))

        (-> refs
            (i/ocall :select "g.tube-rows")
            (i/ocall :selectAll "g.tube-row")
            (i/ocall :data (fn [d]
                             (let [row-count (get-in d [:configuration :tf-config :tube-row-count])]
                               (into-array (mapv (fn [i]
                                                {:x1 (+ 150 (* i 100))
                                                 :y1 100
                                                 :x2 (+ 150 (* i 100))
                                                 :y2 400}) (range row-count))))))
            (draw-tubes))

        (-> refs
            (i/ocall :select "g.tube-rows")
            (i/ocall :selectAll "g.tube-row-label")
            (i/ocall :data (fn [d]
                             (let [row-count (get-in d [:configuration :tf-config :tube-row-count])]
                               (into-array (mapv (fn [i]
                                                   {:x (+ 130 (* i 100))
                                                    :y 90
                                                    :text (str "Row " (inc i))
                                                    :rotate "rotate(0)"
                                                    :class "tube-row-label"}) (range row-count))))))
            (draw-label))

        (-> refs
            (i/ocall :select "g.tube-rows")
            (i/ocall :selectAll "g.tube-numbering-label")
            (i/ocall :data (fn [d]
                             (let [row-count (get-in d [:configuration :tf-config :tube-row-count])]
                               (into-array (mapv (fn [i]
                                                   {:x      (+ 140 (* i 100))
                                                    :y      200
                                                    :rotate (str "rotate(270," (+ 140 (* i 100)) ",200)")
                                                    :text   (str "Tube "
                                                                 (get-in d [:configuration :tf-config :tube-rows i :start-tube])
                                                                 "→"
                                                                 (get-in d [:configuration :tf-config :tube-rows i :end-tube]))
                                                    :class "tube-numbering-label"})
                                                 (range row-count))))))
            (draw-label))

        (-> refs
            (i/ocall :select "g.burner-rows")
            (i/ocall :selectAll "g.burner-row")
            (i/ocall :data (fn [d]
                             (let [row-count (get-in d [:configuration :tf-config :burner-row-count])
                                   start-with (if (= true (get-in d [:configuration :tf-config :burner-first?]))
                                                100
                                                200)]
                               (into-array (mapv (fn [i]
                                                   {:x1 (+ start-with (* i 100))
                                                    :y1 100
                                                    :x2 (+ start-with (* i 100))
                                                    :y2 400}) (range row-count))))))
            (draw-burners))

        (-> refs
            (i/ocall :select "g.burner-rows")
            (i/ocall :selectAll "g.burner-numbering-label")
            (i/ocall :data (fn [d]
                             (let [row-count (get-in d [:configuration :tf-config :burner-row-count])
                                   start-with (if (= true (get-in d [:configuration :tf-config :burner-first?]))
                                                100
                                                200)]
                               (into-array (mapv (fn [i]
                                                   {:x      (- (+ start-with (* i 100)) 8)
                                                    :y      380
                                                    :rotate (str "rotate(270," (- (+ start-with (* i 100)) 8) ",380)")
                                                    :text   (str "Burner "
                                                                 (get-in d [:configuration :tf-config :burner-rows i :start-burner])
                                                                 "→"
                                                                 (get-in d [:configuration :tf-config :burner-rows i :end-burner]))
                                                    :class "burner-numbering-label"})
                                                 (range row-count))))))
            (draw-label)))))

  ;;exit
  (-> coll
      (i/ocall :exit)
      (i/ocall :remove)))

(defn draw-reformer-tf [coll]
  (let [news (i/ocall coll :enter)]
    ;;enter
    (let [refs (-> news
                   (i/ocall :append "g")
                   (i/ocall :attr "class" "reformer"))
          ;walls
          #_(-> refs
              (i/ocall :append "g")
              (i/ocall :attr "class" "walls"))]
      (-> refs
          (i/ocall :append "g")
          (i/ocall :attr "class" "walls"))

      ;;update
      (let [refs (i/ocall refs :merge coll)]
        (-> refs
            (i/ocall :select "g.walls")
            (i/ocall :selectAll "g.side")
            (i/ocall :data (fn [r i]
                             (let [{:keys [width height]} (get-in r [:configuration :dimen])
                                   north {:x      (+ (/ width 2) 50)
                                          :y      45
                                          :text   (get-in r [:configuration :tf-config :wall-names :north])
                                          :rotate "0"}
                                   east {:x      (+ width 105)
                                         :y      230
                                         :text   (get-in r [:configuration :tf-config :wall-names :east])
                                         :rotate (str "90," (+ width 105) "," 230 )}
                                   south {:x    (+ (/ width 2) 50)
                                          :y    465
                                          :text (get-in r [:configuration :tf-config :wall-names :south])
                                          :rotate "0"}
                                   west {:x      48
                                         :y      270
                                         :text   (get-in r [:configuration :tf-config :wall-names :west])
                                         :rotate (str "270," 45 "," 270)}]
                               (clj->js (into-array [north east west south])))))
            (draw-walls))
        (-> refs
            (i/ocall :selectAll "g.container")
            (i/ocall :data (fn [r i]
                                  (array r)))
            (draw-chamber))))

    ;;exit
    (-> coll
        (i/ocall :exit)
        (i/ocall :remove))))

(defn render-init [ele data dimen]
  (-> js/d3
      (i/ocall :select ele)
      (i/ocall :selectAll "g.reformer")
      (i/ocall :data #js[(assoc-in data [:configuration :dimen] dimen)])
      (draw-reformer-tf)))

(defn on-mount [this state]
  (let [ele (dom/dom-node this)
        data (:reformer-data (r/props this))
        dimen {:width (:svg-width (r/props this)) :height (:height (r/props this))}]
    (swap! state assoc :container ele)
    (render-init ele data dimen)))

(defn on-update [this state]
  (let [ele (:container @state)
        data (:reformer-data (r/props this))
        dimen {:width (:svg-width (r/props this)) :height (:height (r/props this))}]
    (render-init ele data dimen)))

(defn reformer-layout-tf [props]
  (let [state (atom {})]
    (r/create-class
      {:reagent-render (fn [props]
                         (let [{:keys [width height view-box]
                                :or   {width "100%" height "100%" :view-box "0 0 600 500"}} props]
                           [:svg
                            {:width  width
                             :height height
                             :view-box "0 0 700 500"
                             :style {:background-color "lightgray"}}]))
       :component-did-mount  (fn [this]
                               (on-mount this state))
       :component-did-update (fn [this _]
                               (on-update this state))
       })))