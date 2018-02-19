;; subscriptions for dialog edit-pyrometer
(ns tta.dialog.edit-pyrometer.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.util.common :as au]
            [tta.app.subs :as app-subs]
            [tta.component.settings.subs :as src-subs]))

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :edit-pyrometer])))

(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _] (:open? dialog)))

(rf/reg-sub
 ::src-data
 :<- [::src-subs/data]
 (fn [settings _] (:pyrometers settings)))

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
 :<- [::form]
 :<- [::data]
 (fn [[form data] [_ path]]
   (or (get-in form path)
       {:value (get-in data path)
        :valid? true})))

(rf/reg-sub
 ::dirty?
 :<- [::data]
 :<- [::src-data]
 (fn [[data src-data] _] (not= data src-data)))

(rf/reg-sub
 ::valid?
 :<- [::form]
 (fn [form _] (au/some-invalid form)))

(rf/reg-sub
 ::can-submit?
 :<- [::dirty?]
 :<- [::valid?]
 (fn [[dirty? valid?] _] (and dirty? valid?)))
