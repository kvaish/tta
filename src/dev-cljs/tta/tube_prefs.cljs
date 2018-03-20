(ns tta.tube-prefs
  (:require [reagent.core :as r]
            [tta.app.input :refer [list-tube-prefs]]))

(defonce tube-count 50)

(defonce my-state (r/atom {:tube-prefs (vec (repeat tube-count nil))}))

(defn tube-pref [index]
  (get-in @my-state [:tube-prefs index]))

(defn set-tube-pref [index pref]
  (swap! my-state assoc-in [:tube-prefs index] pref))

(defn clear-tube-prefs []
  (swap! my-state assoc :tube-prefs (vec (repeat tube-count nil))))

(defn tube-prefs []
  [:div {:style {:height 450}}
   [list-tube-prefs
    {:label "Chamber 1"
     :height 400
     :start-tube 1, :end-tube tube-count
     :on-clear (if (some some? (:tube-prefs @my-state))
                  clear-tube-prefs)
     :selected-fn tube-pref
     :on-select set-tube-pref}]])
