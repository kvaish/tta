;; subscriptions for dialog choose-plant
(ns tta.dialog.choose-plant.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [ht.app.event :as ht-event]
            [tta.util.service :as svc]
            [tta.dialog.choose-plant.event :as cp-event]
            [reagent.ratom :as rr]))

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :choose-plant])))


;;derived signals/subscriptions
(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))

(rf/reg-sub
 ::data
 :<- [::dialog]
 (fn [dialog _]
   (:data dialog)))

(rf/reg-sub
 ::field
 :<- [::dialog]
 (fn [dialog [_ id]]
   (get-in dialog [:field id])))

(rf/reg-sub
 ::fetched
 :<- [::dialog]
 (fn [dialog [_ id]]
   (get-in dialog [:data :fetched])))

(rf/reg-sub-raw
 ::plant-list
 (fn [dba [_]]
   (let [selected-client-id (get-in @dba [:client :active])
         fetched (get-in @dba [:dialog :choose-plant :data :fetched])]
     (if-not fetched
       (svc/fetch-plant-list
        {:client-id selected-client-id
         :evt-success [::cp-event/set-plant-list]
         :evt-failure [::ht-event/service-failure true]}))
     (rr/make-reaction
      (fn [] (get-in @dba [:dialog :choose-plant :data :plant-list]))))))
