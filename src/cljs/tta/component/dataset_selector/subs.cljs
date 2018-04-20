;; subscriptions for component dataset-selector
(ns tta.component.dataset-selector.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [cljs-time.core :as t]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]
            [tta.util.service :as svc]))

;; primary signals
(rf/reg-sub
 ::component
 (fn [db _]
   (get-in db [:component :dataset-selector])))

(rf/reg-sub
 ::filters
 :<- [::component]
 (fn [component _] (:filters component)))

(rf/reg-sub-raw
 ::date-range
 (fn [filters _]
   (reaction
    (let [{:keys [date-range]} @(rf/subscribe [::filters])]
      (or date-range
          (let [date-range {:start (t/ago (t/period :months 1))
                            :end (t/now)}]
            (rf/dispatch [:tta.component.dataset-selector.event/set-filters
                          {:date-range date-range}])
            date-range))))))

(rf/reg-sub
 ::all-datasets
 :<- [::component]
 (fn [component _] (:datasets component)))

(rf/reg-sub
 ::fetched-datasets-info
 :<- [::component]
 (fn [component _] (:fetched-datasets-info component)))

(rf/reg-sub
 ::fetching?
 :<- [::component]
 (fn [component _] (:fetching? component)))

(rf/reg-sub
 ::fetched?
 :<- [::date-range]
 :<- [::app-subs/plant]
 :<- [::fetched-datasets-info]
 (fn [[date-range plant info] _]
   (and (= date-range (:date-range info))
        (= (:id plant) (:plant-id info)))))

(rf/reg-sub-raw
  ::datasets
  (fn [_ _]
    (reaction
     (if @(rf/subscribe [::fetched?])
       @(rf/subscribe [::all-datasets])
       (let [plant @(rf/subscribe [::app-subs/plant])
             date-range @(rf/subscribe [::date-range])]
         (rf/dispatch [:tta.component.dataset-selector.event/fetch-datasets
                       plant date-range])
         [])))))

(rf/reg-sub
  ::data
  :<- [::datasets]
  :<- [::ht-subs/topsoe?]
  (fn [[datasets topsoe?] _]
    (into []
          (map (fn [i]
                 (if topsoe?
                   {:data-date (str (:data-date i))
                    :tubes%    (get-in i [:summary :tubes%])
                    :topsoe?   topsoe?
                    :id (:id i)}
                   (if-not (:topsoe? i)
                     {:data-date (str (:data-date i))
                      :tubes%    (get-in i [:summary :tubes%])
                      :topsoe?   topsoe?
                      :id (:id i)}))) datasets))))
