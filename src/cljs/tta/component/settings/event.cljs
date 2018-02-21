;; events for component setting
(ns tta.component.settings.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field set-text-field
                                            set-field-number
                                            set-field-temperature
                                            validate-field parse-value]]
            [tta.app.event :as app-event]
            [tta.component.settings.subs :as subs]))

(def data-path [:component :settings :data])
(def form-path [:component :settings :form])

(rf/reg-event-fx
 ::init
 (fn [_ _]
   (let [pid (:value @(rf/subscribe [::subs/field [:pyrometer-id]]))]
     {:dispatch [::set-field [:pyrometer-id] pid true]})))

(rf/reg-event-db
 ::close
 (fn [db _]
   (-> db
       (assoc-in form-path nil)
       (assoc-in data-path nil))))

(rf/reg-event-fx
 ::upload
 (fn [{:keys [db]} [_ next-event]]
   ;;TODO: save and then dispatch next-event
   (js/console.log "todo: upload settings")
   (if next-event {:dispatch next-event})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field db path value data data-path form-path required?))))

(rf/reg-event-db
 ::set-text-field
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-text-field db path value data data-path form-path required?))))

(rf/reg-event-fx
 ::set-temp-unit
 (fn [{:keys [db]} [_ %]]
   (let [data @(rf/subscribe [::subs/data])
         target-temp (subs/get-field-temp [:target-temp] nil data %)
         design-temp (subs/get-field-temp [:design-temp] nil data %)]
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
     (au/set-field-number db path value data data-path form-path required?
                       {:max max, :min min}))))

(rf/reg-event-fx
 ::set-pyrometers
 (fn [{:keys [db]} [_ pyrometers]]
   (let [data @(rf/subscribe [::subs/data])
         pid (:value @(rf/subscribe [::subs/field [:pyromter-id]]))
         missing? (not (some #(= pid (:id %)) pyrometers))]
     {:db (assoc-in db data-path (assoc data :pyrometers pyrometers))
      :dispatch-n (list (if missing? [::set-field [:pyrometer-id] nil true]))})))
