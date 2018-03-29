;; subscriptions for dialog tube-prefs
(ns tta.dialog.tube-prefs.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.component.settings.subs :as src-subs]))

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :tube-prefs])))


;;derived signals/subscriptions

(rf/reg-sub
 ::src-data
 :<- [::src-subs/data]
 (fn [settings _]
   (mapv :tube-prefs (or (get-in settings [:sf-settings :chambers])
                         (get-in settings [:tf-settings :tube-rows])))))

(rf/reg-sub
 ::data
 :<- [::dialog]
 :<- [::src-data]
 (fn [[dialog src-data] _]
   (or (:data dialog) src-data)))

(rf/reg-sub
 ::form
 :<- [::dialog]
 (fn [dialog _] (:form dialog)))

(rf/reg-sub
 ::field
 :<- [::data]
 (fn [data [_ path]]
   (get-in data path)))

(rf/reg-sub
 ::dirty?
 :<- [::data]
 :<- [::src-data]
 (fn [[data src-data] _] (not= data src-data)))


(rf/reg-sub
 ::can-submit?
 :<- [::dirty?]
 (fn [dirty? _] dirty?))

(rf/reg-sub
 ::warn-on-close?
 :<- [::dirty?]
 (fn [dirty? _] dirty?))

(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))
