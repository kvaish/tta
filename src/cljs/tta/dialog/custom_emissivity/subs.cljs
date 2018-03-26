;; subscriptions for dialog custom-emissivity
(ns tta.dialog.custom-emissivity.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.component.settings.subs :as src-subs]
            [tta.app.subs :as app-subs]
            [tta.util.common :as au]))

;; primary signals

(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :custom-emissivity])))

(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _] (:open? dialog)))

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
  (fn [[dirty? valid?] _] (and dirty? (if valid? valid? false))))

(rf/reg-sub
  ::warn-on-close?
  :<- [::dirty?]
  :<- [::valid?]
  (fn [[dirty? valid?] _] (or dirty? (not valid?))))

(defn init-raw-data [plant]
  (let [firing (get-in plant [:config :firing])
        config-data  (case firing
                       "side" (get-in plant [:config :sf-config :chambers])
                       "top" (get-in plant [:config :tf-config :tube-rows]))
        tube-count (get-in config-data [0 :tube-count])
        side-data (vec (repeat 2 (vec (repeat tube-count nil))))]
    (case firing
      "side" (vec (repeat (count config-data) side-data))
      "top" (vec (repeat 3 (vec (repeat (count config-data) side-data)))))))

(rf/reg-sub
  ::src-data
  :<- [::src-subs/data]
  :<- [::app-subs/plant]
  (fn [[settings plant] _]
    (let [data (or (get-in settings [:sf-settings :chambers])
                   (get-in settings [:tf-settings :levels]))
          firing (get-in plant [:config :firing])]
      (if (nil? (get-in data [0 :custom-emissivity]))
        (init-raw-data plant)
        (reduce #(conj %1 (case firing
                            "side" (:custom-emissivity %2)
                            "top" (mapv (fn [level-data]
                                          (let [data (:tube-rows data)]
                                            (:custom-emissivity data)))
                                        %2))) [] data)))))

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
  ::selected-level-index
  :<- [::form]
  (fn [form _]
    (or (:selected-level-index form) 0)))

(rf/reg-sub
 ::field
 :<- [::form]
 :<- [::data]
 (fn [[form data] [_ path]]
   (or (get-in form path)
       {:value (get-in data path)
        :valid? true})))

(rf/reg-sub
  ::tab-opts
  (fn [db _]
     ["Top" "Middle" "Bottom"]))

(rf/reg-sub
  ::tube-pref
  :<- [::src-subs/data]
  (fn [settings [_ row index]]
    (or (get-in settings [:tf-settings :tube-rows row :tube-prefs index])
        (get-in settings [:sf-settings :chambers row :tube-prefs index]))))