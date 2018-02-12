;; subscriptions for dialog custom-emissivity
(ns tta.dialog.custom-emissivity.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.component.settings.subs :as setting-subs]))

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :custom-emissivity])))

(rf/reg-sub
 ::settings
 (fn [db _]
   @(rf/subscribe [::setting-subs/data])))

(rf/reg-sub
 ::plant
 (fn [db _] 
   @(rf/subscribe [::setting-subs/plant])))

;;derived signals/subscriptions
(rf/reg-sub
 ::src-custom-emissivity
 :<- [::settings]
 :<- [::plant]
 (fn [[settings plant] _]
   
   (let [firing (get-in plant [:config :firing])
         path (cond
                (= firing "side") [:sf-settings :chambers ]
                (= firing "top")   [:tf-settings :rows])]
     (reduce (fn [coll data]
               (conj coll (:custom-emissivity data))) [] (get-in settings path)))))
(rf/reg-sub
 ::data-custom-emissivity
 :<- [::dialog]
 :<- [::settings]
 :<- [::plant]
 (fn [[dialog settings plant] _]
   
   (let [firing (get-in plant [:config :firing])
         path (cond
                (= firing "side") [:sf-settings :chambers ]
                (= firing "top")   [:tf-settings :rows])]
     (or (get-in dialog [:data :custom-emissivity])
         @(rf/subscribe [::src-custom-emissivity])))))

(rf/reg-sub
 ::firing
 :<- [::plant]
 (fn [plant _]
   (get-in plant [:config :firing])))

(rf/reg-sub
 ::config-data
 :<- [::dialog]
 :<- [::settings]
 :<- [::plant]
 (fn [[dialog settings plant] _]
   
   (let [firing @(rf/subscribe [::firing])
         path (cond
                (= firing "side") [:config :sf-config :chambers]
                (= firing "top")   [:config :tf-config :rows])
         config-data (get-in plant path)]
     (reduce (fn [col {:keys [name start-tube end-tube tube-count side-names]}]
               (conj col (assoc  {}
                                 :name name
                                 :start-tube start-tube
                                 :end-tube end-tube
                                 :side-names side-names))) [] config-data))))
(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))

(rf/reg-sub
 ::field
 :<- [::dialog]
 (fn [dialog [_ id]]
   (get-in dialog [:field id])))


(rf/reg-sub
 ::custom-emissivity-field
 :<- [::data-custom-emissivity]
 :<- [::dialog]
 (fn [[data dialog] [_ chamber side tube]]
   (or (get-in dialog [:form chamber side tube])
       {:value (get-in data [chamber side tube] )})))


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
 (fn [dialog  _]
   (let [error?  (some some-invalid (vals
                                     (:form dialog)))
         emissivity-data (get-in dialog [:data :custom-emissivity]) 
         dirty? (and emissivity-data
                    (not (= emissivity-data
                            @(rf/subscribe[::src-custom-emissivity]))))]
     (if (and (not error?) dirty?)
       true
       false))))
