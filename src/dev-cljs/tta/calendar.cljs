(ns tta.calendar
  (:require [reagent.core :as r]
            [tta.app.calendar :refer [date-picker
                                      date-range-picker
                                      calendar-component]]))

(defonce state (r/atom
                {:start {:day 2, :month 1, :year 2018}
                 :end {:day 30, :month 4, :year 2018}}))

(defn datepickers-test []
  [:div
   [date-picker {:date (:start @state)
                 :valid-range {:max {:day 20, :month 3, :year 2018}
                               :min {:day 20, :month 3, :year 2018}}
                 :on-select #(swap! state assoc :start %)}]
   [date-range-picker {:start (:start @state)
                       :end (:end @state)
                       :valid-range {:max {:day 20, :month 3, :year 2018}
                                     :min {:day 20, :month 3, :year 2018}}
                       :on-select #(swap! state assoc
                                      :start (:start %)
                                      :end (:end %))}]])

(defn calendar-test []
  [:div
   [calendar-component
    {:selection-complete-event (fn [r] (js/console.log r))
     :selection "range"}]])
