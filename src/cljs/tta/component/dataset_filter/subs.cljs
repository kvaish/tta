;; subscriptions for component dataset-filter
(ns tta.component.dataset-filter.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [ht.app.event :as ht-event]
            [reagent.ratom :as rr]
            [tta.util.auth :as auth]
            [tta.util.service :as svc]
            [cljs-time.core :as t]))

;; primary signals
(rf/reg-sub
 ::component
 (fn [db _]
   (get-in db [:component :dataset-filter])))

(rf/reg-sub
  ::date-range
  :<- [::component]
  (fn [component _]
    (:date-range component)))

(rf/reg-sub
  ::dataset
  (fn [db _] ;dataset
    [{:data-date "hello hello hello 1"
      :summary {:tubes% 68}
      :topsoe? true}
     {:data-date "hello 2"
      :summary {:tubes% 90}
      :topsoe?   false}
     {:data-date "hello 3"
      :summary {:tubes% 80}
      :topsoe? true}]))

(rf/reg-sub-raw
  ::dataset-raw
  (fn [_ _]
    (reaction
      (let [date-range @(rf/subscribe [::date-range])
            plant @(rf/subscribe [::app-subs/plant])]
        (svc/find-datasets
          {:client-id (:client-id plant)
           :plant-id (:id plant)
           :start (:start date-range)                                          ;(:start date-range)
           :end (:end date-range)                                            ;(:end date-range)
           :evt-success (js/console.log "success")
           :evt-failure [::ht-event/service-failure false]})))))

(rf/reg-sub
  ::data
  :<- [::dataset]
  :<- [::ht-subs/topsoe?]
  (fn [[dataset topsoe?] _]
    (into []
          (map (fn [i]
                 (if topsoe?
                   {:data-date (:data-date i)
                    :tubes%    (get-in i [:summary :tubes%])
                    :topsoe?   topsoe?}
                   (if-not (:topsoe? i)
                     {:data-date (:data-date i)
                      :tubes%    (get-in i [:summary :tubes%])
                      :topsoe?   topsoe?}))) dataset))))

;; derived signals/subscriptions
