(ns tta.app.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::name
 (fn [db]
   (:name db)))

(rf/reg-sub
 ::view-size
 (fn [db]
   (get-in db [:view :size])))

(rf/reg-sub
 ::language-options
 (fn [db]
   (get-in db [:language :options])))

(rf/reg-sub
 ::active-language-id
 (fn [db]
   (get-in db [:language :active])))

(rf/reg-sub
 ::translate
 (fn [db [_ key-v]]
   (let [{:keys [active translation]} (:language db)]
     (-> translation
         (get active)
         (get-in key-v)))))

