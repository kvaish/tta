(ns tta.calendar
  (:require [tta.app.calendar :refer [date-picker
                                      date-range-picker
                                      calendar-component]]))

(defn datepickers-test []
  [:div
   [date-picker {:date nil}]
   [date-range-picker {:start nil
                       :end nil}]])

(defn calendar-test []
  [:div
   [calendar-component
    {:selection-complete-event (fn [r] (js/console.log r))
     :selection "range"}]])
