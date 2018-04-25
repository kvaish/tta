;; subscriptions for dialog view-factor
(ns tta.dialog.view-factor.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.util.common :as au]
            [tta.app.subs :as app-subs]
            [tta.component.config.subs :as src-subs]))

;; Do NOT use rf/subscribe
;; instead add input signals like :<- [::query-id]
;; or use reaction or make-reaction (refer reagent docs)


;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :view-factor])))

(rf/reg-sub
 ::data
 :<- [::src-data]
 :<- [::dialog]
 (fn [[src-data dialog] _] (or (:data dialog) src-data)))

(rf/reg-sub
 ::form
 :<- [::dialog]
 (fn [dialog _] (:form dialog)))

(rf/reg-sub
 ::view
 :<- [::dialog]
 (fn [dialog _] (:view dialog)))

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
 (fn [[dirty? valid? data index] _] (and dirty? valid?
                                        (not-empty data) (nil? index))))

(rf/reg-sub
 ::warn-on-close?
 :<- [::dirty?]
 :<- [::valid?]
 (fn [[dirty? valid? po-warn?] _] (or dirty? (not valid?) po-warn?)))

;;derived signals/subscriptions
(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))

#_(rf/reg-sub
 ::data
 :<- [::dialog]
 :<- [::src-data]
 (fn [[dialog src-data] _]
   (or (:data dialog)
       src-data)))

(rf/reg-sub
 ::field
 :<- [::form]
 :<- [::data]
 (fn [[form data] [_ path]]
   (or (get-in form path)
       {:value (get-in data path)
        :valid? true})))

(rf/reg-sub
 ::config
 :<- [::src-subs/data]
 (fn [config] config))

(rf/reg-sub
 ::level-opts ;; bottom tab selection options
 :<- [::config]
 (fn [config _]
   (->> [{:id :top
          :label (translate [:data-entry :levels :top] "Top")
          :show? (get-in config [:tf-config :measure-levels :top?])}
         {:id :middle
          :label (translate [:data-entry :levels :middle] "Middle")
          :show? (get-in config [:tf-config :measure-levels :middle?])}
         {:id :bottom
          :label (translate [:data-entry :levels :bottom] "Bottom")
          :show? (get-in config [:tf-config :measure-levels :bottom?])}]
        (filter :show?)
        (vec))))


(rf/reg-sub
 ::selected-level ;; bottom tab selected
 :<- [::view]
 (fn [view _] (or (:selected-level view) 0)))


(rf/reg-sub
 ::selected-level-key
 :<- [::selected-level]
 :<- [::level-opts]
 (fn [[index opts] _] (get-in opts [index :id])))

(rf/reg-sub
 ::level-key
 :<- [::level-opts]
 (fn [opts [_ index]] (get-in opts [index :id])))


(rf/reg-sub
 ::tube-pref
 :<- [::app-subs/plant]
 (fn [plant [_ row index]]
   (let [rows (get-in plant [:settings :tf-settings :tube-rows])]
     (or (get-in rows [row :tube-prefs index])
         (get-in rows [row :tube-prefs index])))))

(rf/reg-sub
 ::fill-all-wall
 :<- [::form]
 (fn [form _]
   (get-in form [:fill-all-wall])))

(rf/reg-sub
 ::fill-all-ceiling
 :<- [::form]
 (fn [form _]
   (get-in form [:fill-all-ceiling])))

(rf/reg-sub
 ::fill-all-floor
 :<- [::form]
 (fn [form _]
   (get-in form [:fill-all-floor])))

(rf/reg-sub
 ::view-factor-field  
 :<- [::data]
 :<- [::form]
 (fn [[data form] [_ level key row side index]]
   (let [path [level :tube-rows row key side index]]
     (or (get-in form path)
         {:value (get-in data path)
          :valid? true}))))

(rf/reg-sub
 ::row-options
 :<- [::config]
 (fn [config _]
   (let [tube-rows (get-in config [:tf-config :tube-rows])]
     (into [{:id -1  :label "All"}]
           (mapv (fn [{:keys [name]} i]
                   {:id i :label name})
                 tube-rows (range))))))

(rf/reg-sub
 ::row-selection
 :<- [::form]
 (fn [form _]
   (or (get form :row-selection)
        -1)))

(rf/reg-sub
 ::src-data
 :<- [::src-subs/data]
 :<- [::level-opts]
 :<- [::config]
 (fn [[data level-opts config] _]
   (let [{:keys [tube-rows tube-row-count]} (:tf-config config)] 
     (or (:view-factor data)
         (->>(mapv (fn [{:keys [id]} ]
                        {id
                         {:tube-rows (mapv (fn [{:keys [tube-count]}]
                                             (let [value (vec (repeat 2 (vec
                                                                         (repeat
                                                                          tube-count
                                                                          nil))))]
                                               (cond-> {:wall value}
                                                 (= id :ceiling)
                                                 (assoc :ceiling value)
                                                 (= id :floor)
                                                 (assoc :floor value))))
                                           tube-rows)}})
                   level-opts)
             (into {}))))))
