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
 (fn [[form data] [_ path]]
   (get-field path form data)))

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
 ::view-factor-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] [_ level-key row-index wall-type side-index tube-index]]
   (get-field [level-key :tube-rows row-index wall-type side-index tube-index]
              form data)))

(rf/reg-sub
 ::row-options
 :<- [::config]
 (fn [config _]
   (let [tube-rows (get-in config [:tf-config :tube-rows])]
     (into [{:id :all
             :label (translate [:view-factor :row-option :all] "All")}]
           (map-indexed #(hash-map :id %1 :label (:name %2))
                        tube-rows)))))

(rf/reg-sub
 ::row-selection
 :<- [::view]
 (fn [view _]
   (get view :row-selection :all)))

(defn init-tube-row [level-key tube-count]
  (let [init-value (->> (repeat tube-count nil)
                        vec (repeat 2) vec)]
    (cond-> {:wall init-value}
      (= level-key :top) (assoc :ceiling init-value)
      (= level-key :bottom) (assoc :floor init-value))))

(defn init-data [level-opts config]
  (let [{:keys [tube-rows]} (:tf-config config)]
    (reduce (fn [m {level-key :id}]
              (assoc m level-key
                     {:tube-rows
                      (mapv #(init-tube-row level-key (:tube-count %))
                            tube-rows)}))
            {} level-opts)))

(rf/reg-sub
 ::has-data?
 :<- [::data]
 :<- [::form]
 (fn [[data form] [_ level-key row-index]]
   (or (some (fn [sides]
               (some #(some some? %) sides))
             (vals (get-in data [level-key :tube-rows row-index])))
       (some (fn [sides]
               (some #(some (comp some? :value) %) sides))
             (vals (get-in form [level-key :tube-rows row-index]))))))

(rf/reg-sub
 ::src-data
 :<- [::level-opts]
 :<- [::config]
 (fn [[level-opts config] _]
   ;; returns {:top {:tube-rows [{:wall [[]] :celing [[]] :floor [[]]}]}, ..}
   (or (get-in config [:tf-config :view-factor])
       ;; if no data, provide default initial value, empty vectors
       (init-data level-opts config))))
