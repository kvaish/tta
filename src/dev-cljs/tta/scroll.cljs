(ns tta.scroll
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [tta.app.scroll :refer [lazy-scroll-box scroll-box]]))

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

       [:div {:style {:display "inline-block"
                      :border "1px solid grey"}}
        [lazy-scroll-box {:height (:height @my-state)
                          :width (:width @my-state)
                          :scroll-height 800, :scroll-width 500
                          :body-style {:background "linear-gradient(to bottom right,sandybrown,lightgreen"}}
         (fn [{:keys [top left]}]
           (list
            [:div {:key "a"
                   :style {:padding 10}} (repeat 300 ".'. ./. .,. ")]
            [:div {:key "b"
                   :style {:top top, :left left
                           :position "absolute"
                           :padding 10
                           :transition "200ms ease-in-out"}}
             [:p [:b "top: " top]]
             [:p [:b "left: " left]]]))]]

       [:div {:style {:display "inline-block"
                      :border "1px solid grey"
                      :margin-left "50px"}}
        [scroll-box {:style {:height 300, :width 200}}
         [:div {:style {:width 400, :padding 10}} (:text @my-state)]]]])))
