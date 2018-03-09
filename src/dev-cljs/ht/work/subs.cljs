;; subscriptions for dialog work
(ns ht.work.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]))

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :work])))


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
 ::tube-prefs
 (fn [db]
   (get-in db [:plant :settings :sf-settings :chambers])))
