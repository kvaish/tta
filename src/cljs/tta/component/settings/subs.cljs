;; subscriptions for component settings
(ns tta.component.settings.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]
            [tta.util.common :as au]))

(rf/reg-sub
 ::src-data
 :<- [::app-subs/plant]
 (fn [plant _] (:settings plant)))

(rf/reg-sub
 ::component
 (fn [db _] (get-in db [:component :settings])))

(rf/reg-sub
 ::show-error? ;; used for hiding errors until first click on submit
 :<- [::component]
 (fn [component _] (:show-error? component)))

(rf/reg-sub
 ::data
 :<- [::component]
 :<- [::src-data]
 (fn [[component src-data]]
   (or (:data component) src-data)))

(rf/reg-sub
 ::form
 :<- [::component]
 (fn [component _] (:form component)))

(defn get-field
  ([path form data] (get-field path form data identity))
  ([path form data parse]
   (or (get-in form path)
       {:value (parse (get-in data path))
        :valid? true})))

(rf/reg-sub
 ::temp-unit
 :<- [::form]
 :<- [::data]
 (fn [[form data] _]
   (:value (get-field [:temp-unit] form data))))

(rf/reg-sub
 ::field
 :<- [::form]
 :<- [::data]
 (fn [[form data] [_ path]] (get-field path form data)))

(defn get-field-temp [path form data temp-unit]
  (get-field path form data #(au/to-temp-unit % temp-unit)))

(rf/reg-sub
 ::field-temp
 :<- [::form]
 :<- [::data]
 :<- [::temp-unit]
 (fn [[form data temp-unit] [_ path]]
   (get-field-temp path form data temp-unit)))

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
 (fn [[dirty? valid?] _] (and dirty? valid?)))

(rf/reg-sub
 ::warn-on-close?
 :<- [::app-subs/config?]
 :<- [::dirty?]
 :<- [::valid?]
 :<- [:tta.dialog.edit-pyrometer.subs/warn-on-close?]
 (fn [[config? dirty? valid? pyro-warn?] _]
   (if config?
     (or dirty? (not valid?)
         pyro-warn?))))

(rf/reg-sub
 ::has-gold-cup-emissivity?
 :<- [::data]
 (fn [data _]
   (some :gold-cup-emissivity (or (get-in data [:sf-settings :chambers])
                                (get-in data [:tf-settings :tube-rows])))))

(rf/reg-sub
 ::has-custom-emissivity?
 :<- [::data]
 (fn [data _]
   (some :custom-emissivity (or (get-in data [:sf-settings :chambers])
                               (get-in data [:tf-settings :tube-rows])))))

(rf/reg-sub
 ::emissivity-types
 :<- [::has-gold-cup-emissivity?]
 :<- [::has-custom-emissivity?]
 :<- [::ht-subs/translation [:settings :emissivity-type :common]]
 :<- [::ht-subs/translation [:settings :emissivity-type :gold-cup]]
 :<- [::ht-subs/translation [:settings :emissivity-type :custom]]
 (fn [[gold-cup? custom? common gold-cup custom] _]
   [{:id "common"
     :name (or common "Common for all tubes")}
    {:id "goldcup", :disabled? (not gold-cup?)
     :name (or gold-cup "Gold cup")}
    {:id "custom", :disabled? (not custom?)
     :name (or custom "Custom for each tube")}]))

(rf/reg-sub
 ::pyrometers
 :<- [::data]
 (fn [data _] (:pyrometers data)))
