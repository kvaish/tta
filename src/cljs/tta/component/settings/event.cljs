;; events for component setting
(ns tta.component.settings.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-number
                                            set-field-temperature
                                            validate-field parse-value]]
            [tta.app.event :as app-event]
            [tta.app.subs :as app-subs]
            [tta.component.settings.subs :as subs]))

(defonce comp-path [:component :settings])
(defonce data-path (conj comp-path :data))
(defonce form-path (conj comp-path :form))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   (let [{:keys [pyrometer-id temp-unit emissivity-type min-tubes%]}
         @(rf/subscribe [::subs/data])]
     {:dispatch-n
      (list [::set-field [:pyrometer-id] pyrometer-id true]
            [::set-temp-unit (or temp-unit au/deg-C)]
            (if-not emissivity-type
              [::set-data-field [:emissivity-type] "common"])
            (if-not min-tubes%
              [::set-data-field [:min-tubes%] 50]))})))

(rf/reg-event-db
 ::close
 (fn [db _] (assoc-in db comp-path nil)))

(rf/reg-event-db
 ::set-data-field
 (fn [db [_ path value]]
   (let [data @(rf/subscribe [::subs/data])]
     (assoc-in db data-path (assoc-in data path value)))))

(rf/reg-event-fx
 ::upload
 (fn [{:keys [db]} _]
   (merge (when @(rf/subscribe [::subs/can-submit?])
            ;;TODO: raise save fx with busy screen and then show confirmation
            (js/console.log "todo: upload settings")
            {})
          {:db (update-in db comp-path assoc :show-error? true)})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field db path value data data-path form-path required?))))

(rf/reg-event-db
 ::set-text
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field-text db path value data data-path form-path required?))))

(rf/reg-event-fx
 ::set-temp-unit
 (fn [{:keys [db]} [_ %]]
   (let [data @(rf/subscribe [::subs/data])
         target-temp (subs/get-field-temp [:target-temp] nil data %)
         design-temp (subs/get-field-temp [:design-temp] nil data %)
         design-temp (if (nil? (:value design-temp))
                       (au/missing-field)
                       design-temp)]
     {:dispatch [::set-field [:temp-unit] %]
      :db (update-in db form-path
                     #(-> %
                          (assoc-in [:target-temp] target-temp)
                          (assoc-in [:design-temp] design-temp)))})))

(rf/reg-event-db
 ::set-temp
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])
         temp-unit @(rf/subscribe [::subs/temp-unit])]
     (set-field-temperature db path value data data-path form-path required? temp-unit))))

(rf/reg-event-db
 ::set-number
 (fn [db [_ path value required? {:keys [max min]}]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field-number db path value data data-path form-path required?
                       {:max max, :min min}))))

(rf/reg-event-fx
 ::set-pyrometers
 (fn [{:keys [db]} [_ pyrometers]]
   (let [data @(rf/subscribe [::subs/data])
         pid (:value @(rf/subscribe [::subs/field [:pyrometer-id]]))
         missing? (not (some #(= pid (:id %)) pyrometers))]
     {:db (assoc-in db data-path (assoc data :pyrometers pyrometers))
      :dispatch-n (list (if missing? [::set-field [:pyrometer-id] nil true]))})))

;;TODO set custom emissivity
#_(rf/reg-event-db
  ::set-custom-emissivity
  (fn [db [_ custom-emissivity]]
    (let [data @(rf/subscribe [::subs/data])
          firing (get-in @(rf/subscribe [:tta.app.subs/plant]) [:config :firing])
          old-custom-emissivity
          (or (get-in db (conj data-path :sf-settings :chambers))
              (get-in db (conj data-path :tf-settings :levels)))
          new-custom-emissivity
          (mapv (fn [col1 col2]
                  (assoc-in col1 (case firing
                                   "side" [:custom-emissivity]
                                   "top" [:tube-rows :custom-emissivity]) col2))
                old-custom-emissivity custom-emissivity)])))
