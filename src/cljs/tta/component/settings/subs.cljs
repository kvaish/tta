;; subscriptions for component settings
(ns tta.component.settings.subs
  (:require [reagent.ratom :refer [reaction]]
            [re-frame.core :as rf]
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
 (fn [[config? dirty? valid?] _]
   (if config?
     (or dirty? (not valid?)))))

(defn has-emissivity? [settings ekey]
  (and (keyword? ekey)
       (some?
        (if-let [chs (get-in settings [:sf-settings :chambers])]
          ;; check side-fired
          (some ekey chs)
          (if-let [rs (->> (get-in settings[:tf-settings :levels])
                           vals
                           (mapcat :tube-rows))]
            ;; top-fired
            (some ekey rs))))))

(rf/reg-sub
 ::has-gold-cup-emissivity?
 :<- [::data]
 (fn [data _]
   (has-emissivity? data :gold-cup-emissivity)))

(rf/reg-sub
 ::has-custom-emissivity?
 :<- [::data]
 (fn [data _]
   (has-emissivity? data :custom-emissivity)))

(defn emissivity-type-opts [gold-cup? custom?]
  (let [lbl-common (translate [:settings :emissivity-type :common]
                              "Common for all tubes")
        lbl-gold-cup (translate [:settings :emissivity-type :gold-cup]
                                "Gold cup")
        lbl-custom (translate [:settings :emissivity-type :custom]
                              "Custom for each tube")]
    [{:id "common", :label lbl-common}
     {:id "goldcup", :label lbl-gold-cup, :disabled? (not gold-cup?)}
     {:id "custom", :label lbl-custom, :disabled? (not custom?)}]))

(rf/reg-sub-raw
 ::emissivity-types
 (fn [_ _]
   (reaction
    (let [gold-cup? @(rf/subscribe [::has-gold-cup-emissivity?])]
      (emissivity-type-opts gold-cup? true)))))

(rf/reg-sub
 ::pyrometers
 :<- [::data]
 (fn [data _] (:pyrometers data)))
