;; events for dialog edit-pyrometer
(ns tta.dialog.edit-pyrometer.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.util.interop :as i]
            [ht.util.common :refer [today-utc]]
            [ht.app.subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field
                                            set-field-text
                                            set-field-decimal
                                            validate-field parse-value]]
            [tta.app.event :as app-event]
            [tta.dialog.edit-pyrometer.subs :as subs]))

(defonce ^:const dlg-path [:dialog :edit-pyrometer])
(defonce ^:const data-path (conj dlg-path :data))
(defonce ^:const form-path (conj dlg-path :form))
(defonce ^:const po-path (conj dlg-path :po))
(defonce ^:const po-data-path (conj po-path :data))
(defonce ^:const po-form-path (conj po-path :form))

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
     (set-field-text db path value po-data po-data-path po-form-path required?))))

(rf/reg-event-db
 ::set-po-decimal
 (fn [db [_ path value required? {:keys [max min precision]}]]
   (let [po-data (get-in db po-data-path)]
     (set-field-decimal db path value po-data po-data-path po-form-path required?
                        {:max max, :min min, :precision precision}))))

(rf/reg-event-db
 ::save-pyrometer
 (fn [db _]
   (let [data @(rf/subscribe [::subs/data])
         can-submit? @(rf/subscribe [::subs/po-can-submit?])
         {:keys [index], pyro :data} (get-in db po-path)]
     (if can-submit?
       (-> db
           (assoc-in data-path (assoc (or data []) index pyro))
           (assoc-in po-path nil))
       (update-in db po-path assoc :show-error? true)))))

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
         id (str (i/ocall (js/Date.) :valueOf))]
     (assoc-in db po-path {:index (count data)
                           :data {:id id
                                  :date-of-calibration (today-utc)}
                           :form {:serial-number (missing-field)
                                  :tube-emissivity (missing-field)
                                  :wavelength (missing-field)
                                  :name (missing-field)}}))))

(rf/reg-event-db
 ::delete-pyrometer
 (fn [db [_ index]]
   (let [data @(rf/subscribe [::subs/data])
         i (if-let [i @(rf/subscribe [::subs/po-index])]
             (if (< i index) i
                 (if (= i index) nil (dec i))))
         data (vec (concat (take index data) (drop (inc index) data)))]
     (cond-> db
       true (assoc-in data-path data)
       i (update-in po-path assoc :index i)
       (nil? i) (assoc-in po-path nil)))))
