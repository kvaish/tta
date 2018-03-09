(ns tta.table
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [tta.app.scroll :refer [lazy-scroll-box scroll-box]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn table [{:keys [
        height, width,
        row-header-width, col-header-height,
        row-count, col-count,
        row-height, col-width,
        row-header-renderer, col-header-renderer, cell-renderer, row-col-corner-renderer,
        table-count, gutter
    ]}]
    (let [
            mystate (r/atom {})
            [table-count-x table-count-y] table-count
            [gutter-width gutter-height] gutter
            ;; calculate scroll height
            scroll-height (- (* table-count-y  (+ col-header-height gutter-height (* row-count row-height))) gutter-height)
            ;; calculate scroll width
            scroll-width (- (* table-count-x  (+ row-header-width gutter-width (* col-count col-width))) gutter-width)
        ]
        [:div 
            [lazy-scroll-box {:height height
                :width width
                :scroll-height scroll-height, :scroll-width scroll-width
                :body-style {:background "linear-gradient(to bottom right,sandybrown,lightgreen"}}
                (fn [{:keys [top left]}]
                    (let [
                            table-height (/ (- height (* gutter-height (dec table-count-y))) table-count-y)
                            table-width (/ (- width (* gutter-width (dec table-count-x))) table-count-x)
                            max-left (- scroll-width width)
                            max-top (- scroll-height height)
                            table-render-width (* col-count col-width)
                            table-render-height (* row-count row-height)
                            scroll-x (* -1 left (/ (- table-render-width (- table-width row-header-width)) max-left))
                            scroll-y (* -1 top (/ (- table-render-height (- table-height col-header-height)) max-top))

                            col-render-start 2
                            col-render-end 4
                            row-render-start 3
                            row-render-end 5
                        ]
                        (js/console.log "Top:" top "Left:" left)
                        
                        [:div {:style {:height height, :width width, :position "absolute" :top top :left left}}                            
                            ;; Rows of tables
                            (map (fn [table-row-no] ^{:key table-row-no}
                                [:div {:style {:position "absolute" :top (* table-row-no (+ gutter-height table-height))}}
                                    ;; Column of tables
                                    (map (fn [table-col-no] ^{:key table-col-no} 
                                        [:div {:style {:position "absolute" :left (* table-col-no (+ gutter-width table-width)) }}
                                            [:div {:style {:height col-header-height}}
                                                [:div {:style {:background-color "aqua"
                                                        :position "absolute" :left 0 :top 0
                                                        :height col-header-height 
                                                        :width row-header-width}} "RC"]
                                                [:div {:style {:background-color "aquamarine" :overflow "hidden"
                                                                :position "absolute" :left row-header-width :top 0
                                                                :height col-header-height 
                                                                :width (- table-width row-header-width)}}
                                                            [:div {:style {:position "absolute" :left scroll-x
                                                                :width table-render-width :background-color "pink"}}
                                                                (map (fn [%]
                                                                    [:div {:style {:display "inline-block" :width col-width :height col-header-height 
                                                                                :vertical-align "top"}}
                                                                        (if (<= col-render-start % col-render-end)
                                                                            (col-header-renderer % [table-row-no table-col-no])
                                                                        )
                                                                    ]
                                                                ) (range col-count))
                                                            ]
                                                        ]
                                            ]

                                            [:div {:style {}}
                                                [:div {:style {:background-color "azure" :overflow "hidden"
                                                        :position "absolute" :left 0 :top col-header-height
                                                        :width row-header-width :height (- table-height col-header-height)}} 
                                                         [:div {:style {:position "absolute" :top scroll-y
                                                                :height table-render-height :width row-header-width :background-color "pink"}}
                                                            (map (fn [%] 
                                                                [:div {:style {:height row-height}} 
                                                                    (if (<= row-render-start % row-render-end)
                                                                        (row-header-renderer % [table-row-no table-col-no])
                                                                    )
                                                                ]
                                                            ) (range row-count))]]
                                                [:div {:style {:overflow "hidden"
                                                        :position "absolute" :left row-header-width :top col-header-height
                                                        :height (- table-height col-header-height) :width (- table-width row-header-width)}}
                                                            [:div {:style {:position "absolute" :top scroll-y :left scroll-x
                                                                :height table-render-height :width table-render-width :background-color "bisque"}}
                                                                (map (fn [rowno] 
                                                                    [:div {:style {:height row-height}} 
                                                                        (if (<= row-render-start rowno row-render-end)
                                                                            (map (fn [colno] 
                                                                                [:div {:style {:display "inline-block" :height row-height :width col-width :vertical-align "top"}} 
                                                                                    (if (<= col-render-start colno col-render-end)
                                                                                        (cell-renderer rowno colno [table-row-no table-col-no])
                                                                                    )
                                                                                ]
                                                                            ) (range col-count))
                                                                        )
                                                                    ]
                                                                ) (range row-count))]
                                                        ]
                                            ]
                                        ]
                                    ) (range table-count-x))
                                ]
                            ) (range table-count-y))
                            
                        ]
                    )
                )]
        ]
    )
)

(defn table-test []
  (let [my-state (r/atom {:text "content"
                          :height 300, :width 200})]
    (fn []
      [:div
       [table {
           :height 400, :width 600
           :row-header-width 50, :col-header-height 40
           :row-count 15, :col-count 15
           :row-height 50 :col-width 70
           :table-count [3 3]
           :gutter [3 3]
           :row-header-renderer (fn [rowno table]
                                    [:div (str "R" rowno)]
                                )
           :col-header-renderer (fn [colno table]
                                    [:div (str "C" colno)]
                                )
           :cell-renderer (fn [rowno colno table]
                                    [:div (str "R" rowno "C" colno)]
                            )
           :row-col-corner-renderer nil
       }]
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
