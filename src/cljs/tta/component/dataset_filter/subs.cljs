;; subscriptions for component dataset-selector
(ns tta.component.dataset-selector.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [ht.app.event :as ht-event]
            [reagent.ratom :as rr]
            [tta.util.auth :as auth]
            [tta.util.service :as svc]
            [tta.component.dataset-selector.event :as event]))

;; primary signals
(rf/reg-sub
 ::component
 (fn [db _]
   (get-in db [:component :dataset-selector])))

(rf/reg-sub
  ::date-range
  :<- [::component]
  (fn [component _]
    (:date-range component)))

(rf/reg-sub-raw
  ::dataset
  (fn [db _]
    ;(reaction)
    (let [date-range @(rf/subscribe [::date-range])
          plant @(rf/subscribe [::app-subs/plant])
          f #(get-in @db [:component :dataset-selector :dataset])
          dataset (f)]
      (svc/find-datasets
        {:client-id   (:client-id plant)
         :plant-id    (:id plant)
         :start       (:start date-range)                   ;(:start date-range)
         :end         (:end date-range)                     ;(:end date-range)
         :evt-success [::event/set-dataset]
         :evt-failure [::ht-event/service-failure false]})
      (rr/make-reaction f))
    ;(rr/make-reaction (constantly nil))
    ))

(rf/reg-sub
  ::data
  :<- [::dataset]
  :<- [::ht-subs/topsoe?]
  (fn [[dataset topsoe?] _]
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
                      :id (:id i)}))) dataset))))

;; derived signals/subscriptions
