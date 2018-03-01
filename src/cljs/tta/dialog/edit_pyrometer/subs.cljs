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
 (fn [form _] (not (au/some-invalid form))))

(rf/reg-sub
 ::can-submit?
 :<- [::dirty?]
 :<- [::valid?]
 :<- [::data]
 :<- [::po-index]
 (fn [[dirty? valid? data index] _] (and dirty? valid?
                                        (not-empty data) (nil? index))))

(rf/reg-sub
 ::warn-on-close?
 :<- [::dirty?]
 :<- [::valid?]
 :<- [::po-warn-on-close?]
 (fn [[dirty? valid? po-warn?] _] (or dirty? (not valid?) po-warn?)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; popover data form to edit a pyrometer

(rf/reg-sub
 ::po
 :<- [::dialog]
 (fn [dialog _] (:po dialog)))

(rf/reg-sub
 ::po-show-error? ;; used to hide errors until first click on accept
 :<- [::po]
 (fn [po _] (:show-error? po)))

(rf/reg-sub
 ::po-index
 :<- [::po]
 (fn [po _] (:index po)))

(rf/reg-sub
 ::po-data
 :<- [::po]
 (fn [po _] (:data po)))

(rf/reg-sub
 ::po-form
 :<- [::po]
 (fn [po _] (:form po)))

(rf/reg-sub
 ::po-field
 :<- [::po-form]
 :<- [::po-data]
 (fn [[form data] [_ path]]
   (or (get-in form path)
       {:value (get-in data path)
        :valid? true})))

(rf/reg-sub
 ::po-dirty?
 :<- [::po-data]
 :<- [::po-index]
 :<- [::data]
 (fn [[pyro index data] _]
   (if index (not= (get data index) pyro))))

(rf/reg-sub
 ::po-valid?
 :<- [::po-form]
 (fn [form _] (not (au/some-invalid form))))

(rf/reg-sub
 ::po-can-submit?
 :<- [::po-dirty?]
 :<- [::po-valid?]
 (fn [[dirty? valid?] _] (and dirty? valid?)))

(rf/reg-sub
 ::po-warn-on-close?
 :<- [::po-dirty?]
 :<- [::po-valid?]
 (fn [[dirty? valid?] _] (or dirty? (not valid?))))
