;; subscriptions for component setting
(ns tta.component.setting.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

;; primary signals
(rf/reg-sub
 ::setting
 (fn [db _]
   (get-in db [:component :setting])))

(rf/reg-sub
 ::plant
 (fn [db _]
   (get-in db [:plant])))

;; derived signals/subscriptions
(rf/reg-sub
 ::plant-setting
 :<- [::plant]
 (fn [plant _]
   (let [firing (get-in plant [:config :firing])
         settings (:settings plant)]
     (case firing
       "side" (dissoc (into settings
                            (:sf-settings settings)) :sf-settings)
       "top" (dissoc (into settings
                            (:tf-settings settings)) :sf-settings)
       nil))))


(rf/reg-sub
 ::get-field
 :<- [::setting]
 (fn [setting [_ path]]
   (let [init-field (get-in (:settings setting) path)
         form-field (get-in (:setting-form setting) path)]
     (or (:value  form-field) init-field))))

