(ns tta.scroll
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [tta.app.scroll :as scroll :refer [lazy-scroll-box scroll-box
                                               lazy-rows lazy-cols lazy-grid]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn scroll-test []
  (let [my-state (r/atom {:text "content"
                          :height 300, :width 200})]
    (fn []
      [:div
       [:div {:style {:padding 10}}
        [:button {:on-click #(swap! my-state update :height + 100)} "height+100"]
        [:button {:on-click #(swap! my-state update :height - 100)} "height-100"]

        [:button {:on-click #(swap! my-state update :text str (repeat 50 "./. .,. .'."))}
         "append text"]
        [:button {:on-click #(swap! my-state update :text
                                    (fn [text] (subs text 0 (- (count text) 500))))}
         "truncate text"]]

       ;; test lazy scroll box
       (let [render-fn
             (fn [{:keys [top left]}]
               (list
                [:div {:key "a"
                       :style {:padding 10}} (repeat 300 ".'. ./. .,. ")]
                [:div {:key "b"
                       :style {:top top, :left left
                               :position "absolute"
                               :padding 10}}
                 [:p [:b "top: " top]]
                 [:p [:b "left: " left]]]))]
         [:div {:style {:display "inline-block"
                        :border "1px solid grey"}}
          [lazy-scroll-box {:height (:height @my-state)
                            :width (:width @my-state)
                            :scroll-height 800, :scroll-width 500
                            :body-style {:background "linear-gradient(to bottom right,sandybrown,lightgreen"}
                            :render-fn render-fn}]])

       ;; test scroll box
       [:div {:style {:display "inline-block"
                      :border "1px solid grey"
                      :margin-left "50px"}}
        [scroll-box {:style {:height 300, :width 200}}
         [:div {:style {:width 400, :padding 10}} (:text @my-state)]]]

       ;; test lazy rows
       (let [width 200, height 300
             item-height 96
             items-render-fn
             (fn [indexes show-item]
               (map (fn [i]
                      [:span {:style {:width width, :height item-height
                                      :display "block"
                                      :border "1px solid lightblue"
                                      :border-radius "8px"}}
                       [:a {:href "#", :on-click #(show-item (dec i))} "prev"]
                       " -" i "- "
                       [:a {:href "#", :on-click #(show-item (inc i))} "next"]])
                    indexes))]
         [:div {:style {:display "inline-block"
                        :border "1px solid grey"
                        :margin-left "50px"}}
          [lazy-rows {:width width, :height height
                      :item-height item-height
                      :item-count 30
                      :items-render-fn items-render-fn}]])

       ;; test lazy cols
       (let [width 300, height 300
             item-width 96
             items-render-fn
             (fn [indexes show-item]
               (map (fn [i]
                      [:span {:style {:width item-width, :height height
                                      :display "block"
                                      :border "1px solid lightblue"  
                                      :border-radius "8px"}}
                       [:a {:href "#", :on-click #(show-item (dec i))} "prev"]
                       " -" i "- "
                       [:a {:href "#", :on-click #(show-item (inc i))} "next"]])
                    indexes))]
         [:div {:style {:display "inline-block"
                        :border "1px solid grey"}}
          [lazy-cols {:height height
                      :width width
                      :item-width item-width
                      :item-count 30
                      :items-render-fn items-render-fn}]])

       ;; test lazy grid
       (let [width 300, height 300
             cell-width 96
             cell-height 96
             cells-render-fn
             (fn [row-list col-list show-cell]
               (map (fn [row]
                      (map (fn [col]
                             [:span {:style {:width cell-width
                                             :height cell-height
                                             :display "block"
                                             :border "1px solid lightblue"
                                             :border-radius "8px"}}
                              [:a {:href "#", :on-click #(show-cell (dec row) col)}
                               "prev-row"]
                              ","
                              [:a {:href "#", :on-click #(show-cell row (dec col))}
                               "prev-col"]
                              " -" row " , " col "- "
                              [:a {:href "#", :on-click #(show-cell (inc row) col)}
                               "next-row"]
                              ","
                              [:a {:href "#", :on-click #(show-cell row (inc col))}
                               "next-col"]])
                           col-list))
                    row-list))]
         [:div {:style {:display "inline-block"
                        :border "1px solid grey"
                        :margin-left "50px"}}
          [lazy-grid {:height height, :width width
                      :cell-width cell-width, :cell-height cell-height
                      :row-count 30, :col-count 30
                      :cells-render-fn cells-render-fn}]])])))
