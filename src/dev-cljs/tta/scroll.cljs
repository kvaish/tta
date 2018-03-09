(ns tta.scroll
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [tta.app.scroll :as scroll :refer [lazy-scroll-box
                                               lazy-list-cols
                                               scroll-box lazy-list-box]]))

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

       ;; test lazy list box
       (let [width (:width @my-state)
             height (:height @my-state)
             item-height 96
             item-width 96
             render-items-fn
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
          [lazy-list-box {:height height
                          :width width
                          :item-height item-height
                          :item-count 30
                          :render-items-fn render-items-fn}]])

       (let [my-state     
             (r/atom {:text "content"
                      :height 300, :width 600}) 
             width (:width @my-state)
             height (:height @my-state)
             item-height 96
             item-width 196
             render-items-fn
             (fn [indexes show-item]
               (map (fn [i]
                      [:span {:style {:width item-width, :height item-height
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
          [lazy-list-cols {:height height
                           :width width
                           :item-width item-width  
                          :item-height item-height
                          :item-count 30
                          :render-items-fn render-items-fn}]])])))
