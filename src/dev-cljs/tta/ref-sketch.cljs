(ns tta.ref-sketch
  (:require [cljs.core.async :refer [<! put!]]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.util.interop :as i]
            [ht.util.common :as u :refer [dev-log]]
            [tta.app.d3 :refer [d3-svg d3-svg-2-string]]
            [tta.component.reformer-dwg.view :as dwg])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(def reformer-data-tf
  (r/atom {:name          "Reformer",
           :version       4,
           :firing        "top",
           :date-modified "2018-01-04T04:49:22.949Z",
           :modified-by   "bhka@topsoe.com",
           :tf-config
           {:wall-names       {:north "North"
                               :east  "East"
                               :west  "West"
                               :south "South"}
            :burner-first?    true
            :tube-row-count   6,
            :tube-rows        (repeat 6 {:tube-count 10
                                         :start-tube 1
                                         :end-tube   10})
            :burner-row-count 7
            :burner-rows      (repeat 7 {:burner-count 5
                                         :start-burner 1
                                         :end-burner   5})
            :section-count    2,
            :sections         [{:tube-count   5
                                :burner-count 3}
                               {:tube-count   5
                                :burner-count 2}]
            :measure-levels   {:top?    true
                               :middle? true
                               :bottom? true}}}))


(def my-state (r/atom {:data {:name "abc"
                              :list [{:x 0, :y 0, :w 10, :h 10}
                                     {:x 10, :y 10, :w 10, :h 10}]}}))

(def my-sketch
  {:width "200px", :height "300px"
   :view-box "0 0 200 300"
   :style {:color "white"
           :font-size "32px"}
   :node {:tag :g
          :attr {:fill "none"
                 :stroke "none"}
          :class :root
          :on-off-classes {:on "reformer", :off "reactor"}
          :nodes [{:tag :rect, :class :back
                   :attrs {:x 0, :y 0, :width 200, :height 300
                           :fill "aliceblue"}}
                  {:tag :text, :class :label
                   :attrs {:x 20, :y 20, :fill "indigo"}
                   :text :name
                   :data #(select-keys % [:name])
                   :did-update #(js/console.log "updated label")}
                  {:tag :text, :class :sub-lable
                   :attrs {:x 20, :y 50, :fill "indigo"}
                   :text :name
                   :data #(select-keys % [:name])
                   :skip? :name}
                  {:tag :rect, :class :child
                   :multi? true
                   :data [:list]
                   :attrs {:x :x, :y :y, :width :w, :height :h
                           :fill "red"}
                   :did-update #(js/console.log "updated rect")}]}})

(defn d3-sketch []
  [d3-svg (assoc my-sketch
           :data (:data @my-state))])

(defn save-image []
  (let [svg-string (d3-svg-2-string (-> my-sketch
                                        (assoc-in [:style :font-family] "open_sans")
                                        (assoc :data (:data @my-state))))]
    ;; (dev-log svg-string)
    #_(dev-log (str "data:image/svg+xml;base64,"
                  (js/btoa (js/unescape (js/encodeURIComponent svg-string)))))
    (go
      (let [res (<! (u/save-svg-to-file "my-sketch.png" svg-string 200 300 60))]
        (dev-log "save image status: " (name res))))))

(defn ref-sketch []
  [:div {:style {:padding "50px"
                 :background "lightblue"}}
   ;; [d3-sketch {}]
   [dwg/reformer-dwg {:width "600px" :height "300px"
                      :preserve-aspect-ratio "none"
                      :config @reformer-data-tf}]
   [ui/flat-button {:label "Save"
                    :on-click #(dwg/save-image {:config @reformer-data-tf})}]])
