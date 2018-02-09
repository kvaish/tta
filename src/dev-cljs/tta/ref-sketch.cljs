(ns tta.ref-sketch
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.util.interop :as i]
            [tta.component.reformer-layout.view :refer [reformer-layout]]
            [tta.app.d3 :refer [d3-svg]]
            [tta.component.reformer-dwg.view :refer [reformer-dwg]]))

(def my-state (r/atom {:data {:name "abc"
                              :list [{:x 0, :y 0, :w 10, :h 10}
                                     {:x 10, :y 10, :w 10, :h 10}]}}))

(defn d3-sketch []
  (let [{:keys [data]} @my-state]
    [d3-svg {:width "200px", :height "300px"
             :view-box "0 0 200 300"
             :style {:background "sandybrown"
                     :color "white"
                     :font-size "32px"
                     :fill "white"
                     :stroke "none"}
             :node {:tag :g
                    :class :root
                    :on-off-classes {:on "reformer", :off "reactor"}
                    :nodes [{:tag :text, :class :label
                             :attrs {:x 20, :y 20}
                             :text :name
                             :data #(select-keys % [:name])
                             :did-update #(js/console.log "updated label")}
                            {:tag :text, :class :sub-lable
                             :attrs {:x 20, :y 50}
                             :text :name
                             :data #(select-keys % [:name])
                             :skip? :name}
                            {:tag :rect, :class :child
                             :multi? true
                             :data [:list]
                             :attrs {:x :x, :y :y, :width :w, :height :h
                                     :fill "red"}
                             :did-update #(js/console.log "updated rect")}]}
             :data data}]))

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

(defn ref-sketch []
  [:div {:style {:padding "50px"
                 :background "lightblue"}}
   ;[d3-sketch {}]
   [reformer-dwg {:width "600px" :height "500px"
                  :config @reformer-data-tf
                  }]])
