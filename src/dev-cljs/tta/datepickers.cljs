(ns tta.datepickers
  (:require [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [reagent.dom :as dom]
            [tta.calendar :as calendar]))

(defn date-picker [{:keys [date]}]
  (let [mystate (r/atom {})
        get-label (fn [{:keys [day month year]}]
          (str day " " (if month (subs ((calendar/month-names) (dec month)) 0 3)) " " year)
        )]
    (r/create-class
    {
      :component-did-mount (fn [this] 
          (swap! mystate assoc :anchor (dom/dom-node this))
          (swap! mystate assoc :open? false)
          (swap! mystate assoc :date date)
        )

      :reagent-render (fn []
        [:div {:style {:display "inline-block" :vertical-align "top"}}
          [app-comp/action-input-box {
            :disabled? false
            :width "150px"
            :label (get-label (:date @mystate))
            :action #(swap! mystate assoc :open? true)
            :right-icon ic/dropdown
            :right-action #(swap! mystate assoc :open? true)}]
            (if (:open? @mystate)
              [app-comp/popover {
                :open (:open? @mystate)
                :on-request-close #(swap! mystate assoc :open? false)
                :anchor-el (:anchor @mystate) 
                :anchor-origin {:horizontal "right", :vertical "bottom"}
                :target-origin {:horizontal "right", :vertical "top"}}
                [calendar/calendar-component {
                                              :selection-complete-event (fn [{:keys [start]}] 
                                                  (js/console.log start)
                                                  (swap! mystate assoc :date start)
                                                  (swap! mystate assoc :open? false)
                                                )
                                              :selection "date"
                                             }]]
            )
            ]
      )
    })
  )
)

(defn date-range-picker [{:keys [start end]}]
  (let [
      mystate (r/atom {})
      get-label (fn [{:keys [start end]}]
        (str (:day start) " " 
            (if (:month start) (subs ((calendar/month-names) (dec (:month start))) 0 3)) " " 
            (:year start) " - " 
            (:day end) " " 
            (if (:month end) (subs ((calendar/month-names) (dec (:month end))) 0 3))  " " 
            (:year end))
      )
    ]
    (r/create-class
    {
      :component-did-mount (fn [this] 
          (swap! mystate assoc :anchor (dom/dom-node this))
          (swap! mystate assoc :open? false)
          (swap! mystate assoc :range {:start start :end end})
        )

      :reagent-render (fn []
        [:div {:style {:display "inline-block" :vertical-align "top"}}
          [app-comp/action-input-box {
            :disabled? false
            :width "200px"
            :label (get-label (:range @mystate))
            :action #(swap! mystate assoc :open? true)
            :left-icon ic/nav-left
            :left-action (fn [%]
              (swap! mystate assoc-in [:range :start] (calendar/add-days (get-in @mystate [:range :start]) {:days 0 :months 0 :years 0}))
              (swap! mystate assoc-in [:range :end] (calendar/add-days (get-in @mystate [:range :end]) {:days 0 :months 0 :years 0}))
            )
            :right-icon ic/nav-right
            :right-action (fn [%]
              (swap! mystate assoc-in [:range :start] (calendar/add-days (get-in @mystate [:range :start]) {:days 2 :months 0 :years 0}))
              (swap! mystate assoc-in [:range :end] (calendar/add-days (get-in @mystate [:range :end]) {:days 2 :months 0 :years 0}))
            )}]
            (if (:open? @mystate)
              [app-comp/popover {
                :open (:open? @mystate)
                :on-request-close #(swap! mystate assoc :open? false)
                :anchor-el (:anchor @mystate) 
                :anchor-origin {:horizontal "right", :vertical "bottom"}
                :target-origin {:horizontal "right", :vertical "top"}}
                [calendar/calendar-component {
                                              :selection-complete-event (fn [r]
                                                (js/console.log r)
                                                (swap! mystate assoc :range r)
                                                (swap! mystate assoc :open? false)
                                              )
                                              :selection "range"
                                             }]]
            )]
      )
    })
  )
)

(defn datepickers-test []
  [:div
    [date-picker {:date nil}]
    [date-range-picker {
      :start nil
      :end nil
    }]]
)
