;; events for dialog edit-pyrometer
(ns tta.dialog.edit-pyrometer.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.subs :refer [translate]]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field
                                            set-text-field set-field-decimal
                                            validate-field parse-value]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.dialog.edit-pyrometer.subs :as subs]))

(def ^:const dlg-path [:dialog :edit-pyrometer])
(def ^:const data-path (conj dlg-path :data))
(def ^:const form-path (conj dlg-path :form))
(def ^:const po-path (conj dlg-path :po))
(def ^:const po-data-path (conj po-path :data))
(def ^:const po-form-path (conj po-path :form))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db dlg-path merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db dlg-path merge {:form nil, :data nil, :po nil}
              options {:open? false})))

(rf/reg-event-fx
 ::submit
 (fn [_ _]
   (let [data @(rf/subscribe [::subs/data])]
     {:dispatch-n (list
                   [:tta.component.settings.event/set-pyrometers data]
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
 ::set-po-field
 (fn [db [_ path value required?]]
   (let [po-data (get-in db po-data-path)]
     (set-field db path value po-data po-data-path po-form-path required?))))

(rf/reg-event-db
 ::set-po-text
 (fn [db [_ path value required?]]
   (let [po-data (get-in db po-data-path)]
     (set-text-field db path value po-data po-data-path po-form-path required?))))

(rf/reg-event-db
 ::set-po-decimal
 (fn [db [_ path value required? {:keys [max min]}]]
   (let [po-data (get-in db po-data-path)]
     (set-field-decimal db path value po-data po-data-path po-form-path required?
                        {:max max, :min min}))))

(rf/reg-event-db
 ::save-pyrometer
 (fn [db _]
   (let [data @(rf/subscribe [::subs/data])
         {:keys [index], pyro :data} (get-in db po-path)]
     (-> db
         (assoc-in data-path (assoc data index pyro))
         (assoc-in po-path nil)))))

(rf/reg-event-db
 ::cancel-pyrometer-edit
 (fn [db _]
   (assoc-in db po-path nil)))

(rf/reg-event-db
 ::edit-pyrometer
 (fn [db [_ pyro index]]
   (assoc-in db po-path {:open? true, :index index, :data pyro})))

(rf/reg-event-db
 ::new-pyrometer
 (fn [db _]
   (let [data @(rf/subscribe [::subs/data])
         id ""]
     (assoc-in db po-path {:open? true
                           :index (count data)
                           :data {:id id}
                           :form {:serial-number (missing-field)
                                  :tube-emissivity (missing-field)
                                  :date-of-calibration (missing-field)
                                  :wavelength (missing-field)
                                  :name (missing-field)}}))))

(rf/reg-event-db
 ::delete-pyrometer
 (fn [db [_ index]]
   (let [data @(rf/subscribe [::subs/data])
         i @(rf/subscribe [::subs/index])
         data (vec (concat (take index data) (drop (inc index) data)))]
     (cond-> (assoc-in db data-path data)
       (= i index) (assoc-in po-path nil)))))
