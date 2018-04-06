;; subscriptions for dialog dataset-settings
(ns tta.dialog.dataset-settings.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.util.common :as au]
            [tta.app.subs :as app-subs]))

;; Do NOT use rf/subscribe
;; instead add input signals like :<- [::query-id]
;; or use reaction or make-reaction (refer reagent docs)

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :dataset-settings])))

(rf/reg-sub
 ::src-data
 :<- [::dialog]
 (fn [dialog _] (:settings dialog)))

(rf/reg-sub
 ::draft
 (fn [db _] (get-in db [:dialog :draft])))

;;derived signals/subscriptions
(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))

(rf/reg-sub
 ::show-error? ;; used for hiding errors until first click on submit
 :<- [::dialog]
 (fn [dialog _] (:show-error? dialog)))

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
 :<- [::dirty?]
 :<- [::valid?]
 (fn [[dirty? valid?] _] (or dirty? (not valid?))))


(rf/reg-sub
 ::data
 :<- [::dialog]
 :<- [::src-data]
 (fn [[dialog src-data] _]
   (or (:data dialog)
       src-data)))

(rf/reg-sub
 ::form
 :<- [::dialog]
 (fn [dialog _] (:form dialog)))

(defn get-field
  ([path form data] (get-field path form data identity))
  ([path form data parse]
   (or (get-in form path)
       {:value (parse (get-in data path))
        :valid? true})))

(rf/reg-sub
 ::field
 :<- [::form]
 :<- [::data]
 (fn [[form data] [_ path]] (get-field path form data)))

;;;plant ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::config
 :<- [::app-subs/plant]
 (fn [plant _] (:config plant)))

(rf/reg-sub
 ::settings
 :<- [::app-subs/plant]
 (fn [plant _] (:settings plant)))

(rf/reg-sub
 ::firing
 :<- [::config]
 (fn [config _] (:firing config)))

(rf/reg-sub
 ::pyrometers
 :<- [::settings]
 :<- [::draft]
 (fn [[settings draft] _]
   (let [p (:pyrometer draft)
         ps (:pyrometers settings)]
     (if (and p (not (some #(= (:id p) (:id %)) ps)))
       (conj ps p)
       ps))))

(rf/reg-sub
 ::has-gold-cup-emissivity?
 :<- [::settings]
 :<- [::firing]
 (fn [[settings firing] _]
   (some :gold-cup-emissivity (if (= firing "side")
                                (get-in settings [:sf-settings :chambers])
                                (some :tube-rows
                                      (get-in settings [:tf-settings :levels]))))))

(rf/reg-sub
 ::has-custom-emissivity?
 :<- [::settings]
 :<- [::firing]
 (fn [[settings firing] _]
   (some :custom-emissivity (if (= firing "side")
                              (get-in settings [:sf-settings :chambers])
                              (some :tube-rows
                                    (get-in settings [:tf-settings :levels]))))))

(rf/reg-sub
 ::active-pyrometer
 :<- [::data]
 (fn [data _] (:pyrometer data)))

(rf/reg-sub
 ::emissivity-type
 :<- [::data]
 (fn [data _] (:emissivity-type data)))

(rf/reg-sub
 ::emissivity
 :<- [::data]
 (fn [data _] (:emissivity data)))


(rf/reg-sub-raw
 ::emissivity-types
 (fn [_ _]
   (reaction
    (let [gold-cup? @(rf/subscribe [::has-gold-cup-emissivity?])
          custom? @(rf/subscribe [::has-custom-emissivity?])
          lbl-common (translate [:settings :emissivity-type :common]
                                    "Common for all tubes")
          lbl-gold-cup (translate [:settings :emissivity-type :gold-cup]
                                      "Gold cup")
          lbl-custom (translate [:settings :emissivity-type :custom]
                                    "Custom for each tube")]
      [{:id "common" :label lbl-common}
       {:id "goldcup" :label lbl-gold-cup :disabled? (not gold-cup?)}
       {:id "custom" :label lbl-custom :disabled? (not custom?)}]))))

(rf/reg-sub
 ::hour-opts
 (fn [_ _]
   (reduce #(conj %1  {:id %2 :name %2}) [] (range 1 25))))

(rf/reg-sub
 ::min-opts
 (fn [_ _]
   (reduce #(conj %1  {:id (* 5 %2) :name (* 5 %2)}) [] (range 1 13))))
