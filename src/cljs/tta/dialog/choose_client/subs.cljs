;; subscriptions for dialog choose-client
(ns tta.dialog.choose-client.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]))

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :choose-client])))

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
 ::clients
 :<- [::data]
 (fn [data _] (:clients data)))

(rf/reg-sub
 ::more?
 :<- [::data]
 (fn [data _] (:more? data)))

(rf/reg-sub
 ::busy?
 :<- [::data]
 (fn [data _] (:busy? data)))
