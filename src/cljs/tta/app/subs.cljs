(ns tta.app.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::view-size
 (fn [db]
   (get-in db [:view-size])))

(rf/reg-sub
 ::language-options
 (fn [db]
   (get-in db [:language :options])))

(rf/reg-sub
 ::active-language
 (fn [db]
   (get-in db [:language :active])))

(rf/reg-sub
 ::translate
 (fn [db [_ key-v]]
   (let [{:keys [active translation]} (:language db)]
     (-> translation
         (get active)
         (get-in key-v)))))

(rf/reg-sub
 ::auth-claims
 (fn [db _]
   (get-in db [:auth :claims])))

(rf/reg-sub
 ::auth-token
 (fn [db _]
   (get-in db [:auth :token])))

(rf/reg-sub
 ::active-client
 (fn [db _]
   (let [{:keys [active] :as client} (:client db)]
     (get client active))))

(rf/reg-sub
 ::active-plant
 (fn [db _]
   (let [{:keys [active] :as plant} (:plant db)]
     (get plant active))))

(rf/reg-sub
 ::busy?
 (fn [db _]
   (get-in db [:busy?])))
