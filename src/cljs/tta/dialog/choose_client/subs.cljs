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
 ::client-list
 :<- [::dialog]
 (fn [dialog _]
   (get-in dialog [:data :clients])))

(rf/reg-sub
 ::selected-client
 :<- [::dialog]
 (fn [dialog _]
   (get-in dialog [:data :selected-client])))

