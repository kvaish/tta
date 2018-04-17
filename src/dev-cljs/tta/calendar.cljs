(ns tta.calendar
  (:require [reagent.core :as r]
            [tta.app.calendar :refer [date-picker
                                      date-range-picker
                                      calendar-component]]))

(defn datepickers-test []
  (let [mystate (r/atom {})]
    (swap! mystate assoc 
      ;; datepicker data
      :dp-date {:day 15, :month 5, :year 2018}
      :dp-min {:day 10, :month 5, :year 2018}
      :dp-max {:day 20, :month 5, :year 2018}
      ;; daterangepicker data
      :drp-start {:day 15, :month 2, :year 2018}
      :drp-end {:day 20, :month 2, :year 2018}
      :drp-type :custom ; :custom/:week/:month/:six-month/:year
      :drp-min {:day 27, :month 1, :year 2018}
      :drp-max {:day 28, :month 2, :year 2020})

    (fn []
      [:div
        [:button {:on-click 
          #(swap! mystate assoc :dp-date {:day 15, :month 3, :year 2018})} 
          "Reset"]
        [date-picker {
          :date (:dp-date @mystate)
          :on-change #(swap! mystate assoc :dp-date %)
          :min (:dp-min @mystate)
          :max (:dp-max @mystate)}]

        [:button {:on-click 
          #(swap! mystate assoc 
              :drp-start {:day 15, :month 3, :year 2018}
              :drp-end {:day 20, :month 3, :year 2018}
              :drp-type :custom)}
          "Reset"]
        [date-range-picker {
          :selected-range { :start (:drp-start @mystate)
                            :end (:drp-end @mystate)
                            :type (:drp-type @mystate)}
          :on-change (fn [{:keys [start end type]}]
            (swap! mystate assoc 
              :drp-start start, :drp-end end, :drp-type type))
          :min (:drp-min @mystate)
          :max (:drp-max @mystate)}]])))

(defn calendar-test []
  [:div
   [calendar-component
    {:selection-complete-event (fn [r] (js/console.log r))
     :selection "range"}]])
