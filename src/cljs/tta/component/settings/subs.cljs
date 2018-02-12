;; subscriptions for component setting
(ns tta.component.settings.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

;; primary signals

(rf/reg-sub
 ::plant
 (fn [db _]

   (get-in db [:plant])))

(rf/reg-sub
 ::settings
 (fn [db _]
   #_(get-in db [:component :settings])
   (or (get-in db [:component :settings])
       (get-in db [:plant :settings])) ))
;; derived signals/subscriptions
(rf/reg-sub
 ::data
 :<- [::plant]
 :<- [::settings]
 (fn [[plant settings] _]
   (or (:data settings)
       (:settings plant))))


(rf/reg-sub
 ::get-field
 :<- [::settings]
 (fn [setting [_ path]]
   (let [init-field (get-in @(rf/subscribe [::data]) path)
         form-field (get-in (:form setting) path)]
     (or form-field {:value init-field}))))
