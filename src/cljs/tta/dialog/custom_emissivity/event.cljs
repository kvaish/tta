;; events for dialog custom-emissivity
(ns tta.dialog.custom-emissivity.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field
                                            set-field-text
                                            set-field-decimal
                                            validate-field parse-value]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.dialog.custom-emissivity.subs :as subs]
            [tta.app.subs :as app-subs]))

(defonce ^:const dlg-path [:dialog :custom-emissivity])
(defonce ^:const data-path (conj dlg-path :data))
(defonce ^:const form-path (conj dlg-path :form))
;(def plant @(rf/subscribe [::app-subs/plant]))
;(def firing (get-in plant [:config :firing]))
#_(def config-data  (case firing
                    "side" (get-in plant [:config :sf-config :chambers])
                    "top" (get-in plant [:config :tf-config :tube-rows])
                    "default"))

(rf/reg-event-db
  ::open
  (fn [db [_ options]]
    (update-in db dlg-path merge options {:open? true
                                          :data @(rf/subscribe [::subs/data])})))

(rf/reg-event-db
  ::close
  (fn [db [_ options]]
    (update-in db dlg-path merge {:form nil, :data nil}
               options {:open? false})))

(rf/reg-event-fx
  ::submit
  (fn [_ _]
    (let [data @(rf/subscribe [::subs/data])]
      {:dispatch-n (list
                     [:tta.component.settings.event/set-custom-emissivity data]
                     [::close])})))

(rf/reg-event-db
  ::set-data
  (fn [db [_ data]]
    (assoc-in db data-path data)))

(rf/reg-event-db
  ::set-options
  (fn [db [_ options]]
    (update-in db dlg-path merge options)))

(rf/reg-event-db
  ::set-field
  (fn [db [_ path value required? {:keys [max min precision]}]]
    (let [data @(rf/subscribe
                 [:tta.dialog.custom-emissivity.subs/data])]
      (set-field-decimal db path value data
                         data-path form-path required?
                         {:max max, :min min, :precision precision}))))

(rf/reg-event-db
 ::reset-dialog
 (fn [db _]
   (update-in db  dlg-path dissoc :form :data :field)))

(rf/reg-event-fx
 ::discard-data
 (fn [db]
   {:dispatch-n  (list [::reset-dialog]
                       [::close])}))

(rf/reg-event-db
  ::set-fill-all-field
  (fn [db [_ path value required? {:keys [max min precision]}]]
    (let [data @(rf/subscribe
                  [:tta.dialog.custom-emissivity.subs/data])]
      (set-field-decimal db path value nil
                         nil form-path required?
                         {:max max, :min min, :precision precision}))))

(rf/reg-event-db
  ::set-level-index
  (fn [db [_ index]]
    (assoc-in db (conj form-path :selected-level-index) index)))

(rf/reg-event-db
 ::fill-all
 (fn [db [_]]
   (let [emissivity @(rf/subscribe [::subs/field [:fill-all]])
         level @(rf/subscribe [::subs/selected-level-index])
         plant @(rf/subscribe [::app-subs/plant])
         firing (get-in plant [:config :firing])
         config-data  (case firing
                        "side" (get-in plant [:config :sf-config :chambers])
                        "top" (get-in plant [:config :tf-config :tube-rows]))
         tube-count (get-in config-data [0 :tube-count])
         side-data (case firing
                     "side" (vec (repeat 2 (vec (repeat tube-count (js/Number (:value emissivity))))))
                     "top" (vec (repeat 3 (vec (repeat 2 (vec (repeat tube-count (js/Number (:value emissivity)))))))))
         form-data (vec (repeat 2 (vec (repeat tube-count  {:valid? true
                                                            :value (js/Number(:value emissivity))}))))]
     (-> db
         (assoc-in data-path
                   (vec (repeat (count config-data) side-data)))
         #_(assoc-in [:dialog :custom-emissivity :form]
                   (vec (repeat (count config-data) form-data)))))))


(rf/reg-event-db
  ::clear-custom-emissivity
  (fn [db [_ col-index]]
    (let [data @(rf/subscribe [::subs/data])
          level @(rf/subscribe [::subs/selected-level-index])
          plant @(rf/subscribe [::app-subs/plant])
          firing (get-in plant [:config :firing])
          config-data  (case firing
                         "side" (get-in plant [:config :sf-config :chambers])
                         "top" (get-in plant [:config :tf-config :tube-rows]))
          tube-count (get-in config-data [0 :tube-count])
          side-data (vec (repeat 2 (vec (repeat tube-count nil))))]
      (case firing
        "side" (-> db
                   (assoc-in (conj data-path col-index) side-data)
                   (assoc-in (conj form-path col-index) side-data))
        "top" (-> db
                  (assoc-in (conj data-path level col-index) side-data)
                  (assoc-in (conj form-path level col-index) side-data))))))

