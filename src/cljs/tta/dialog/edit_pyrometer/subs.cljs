;; subscriptions for dialog edit-pyrometer
(ns tta.dialog.edit-pyrometer.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.component.settings.subs :as setting-subs]))

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :edit-pyrometer])))

(rf/reg-sub
 ::settings
 (fn [db _] @(rf/subscribe [::setting-subs/data])))
;;derived signals/subscriptions

(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))

(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))

(rf/reg-sub
 ::pyrometers
 :<- [::dialog]
 :<- [::settings]
 (fn [[dialog settings] _]
   (or (:data dialog)
       (:pyrometers settings))))

#_(rf/reg-sub
 ::dialog
 :<- [::pyrometers]
 (fn [db _]
   (get-in db [:dialog :data :pyrometers])))


(rf/reg-sub
 ::data
 :<- [::dialog]
 (fn [dialog [_ id]]
   (get-in dialog [:data])))

(rf/reg-sub
 ::field
 :<- [::dialog]
 (fn [dialog [_ id]]
   (get-in dialog [:field id])))

(rf/reg-sub
 ::popover-open?
 :<- [::dialog]
 (fn [dialog _]
   (get-in dialog [:popover-open?])))

(rf/reg-sub
 ::get-field
 :<- [::dialog]
 (fn [dialog [_ path]]
   (get-in dialog path)))

(rf/reg-sub
 ::get-pyrometer-field
 :<- [::dialog]
 (fn [dialog [_ field-path id]]
   (let [pyrometers @(rf/subscribe [::pyrometers])
         form-data  @(rf/subscribe [::get-field [:form]])
         res-data   (reduce (fn [m v]
                              (if (= (:id v) id)
                                (conj m v)
                                m))
                            [] pyrometers)

         field-data (reduce (fn [m v]
                              (if (= (key v) id)
                                (conj m v)
                                m))
                            [] form-data)]
         
     (if-not (empty? (first field-data))
       (get-in (last (first field-data)) field-path) 
       {:value (get-in  (first  res-data) field-path)}))))

(rf/reg-sub
 ::src-get-pyrometer
 :<- [::settings]
 (fn [settings [_ pyrometer-id]]
   (let [pyrometers (:pyrometers settings)]
     (first (filter (fn [{:keys [id]}]
                      (= id pyrometer-id)) pyrometers)))))

(rf/reg-sub
 ::data-get-pyrometer
 :<- [::dialog]
 (fn [{:keys [data]} [_ pyrometer-id]]
   (let [pyrometers data]
     (first (filter (fn [{:keys [id]}]
                      (= id pyrometer-id)) pyrometers)))))

(defn some-invalid [form]
  (if (map? form)
    (do
      (if (boolean? (:valid? form))
        (not (:valid? form))
        (some some-invalid  (vals form))))
    (some some-invalid form)))


(rf/reg-sub
 ::valid-dirty?
 :<- [::dialog]
 (fn [dialog  [_ pyrometer-id]]
   (let [error?  (some some-invalid (vals
                                     (get (:form dialog) pyrometer-id) ))
         src-pyrometer @(rf/subscribe [::src-get-pyrometer pyrometer-id])
         data-pyrometer @(rf/subscribe [::data-get-pyrometer pyrometer-id])
         dirty? (and (not-empty data-pyrometer) 
                     (not (= src-pyrometer
                             data-pyrometer)))]
     
     (if (and (not error?) (cond pyrometer-id
                                 (nil? src-pyrometer) true
                                 dirty?) )
       true
       false))))


#_(rf/reg-sub-raw
   ::pyrometers
   (fn [dba _]
     (let [data #(get-in @dba [:dialog :edit-pyrometer :data])]
       (if-not data
         )
       )))
