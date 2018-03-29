;; events for dialog custom-emissivity
(ns tta.dialog.custom-emissivity.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field
                                            set-field-text
                                            set-field-decimal
                                            validate-field parse-value]]
            [tta.app.event :as app-event]
            [tta.app.subs :as app-subs]
            [tta.dialog.custom-emissivity.subs :as subs]))

(defonce ^:const dlg-path [:dialog :custom-emissivity])
(defonce ^:const data-path (conj dlg-path :data))
(defonce ^:const form-path (conj dlg-path :form))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db dlg-path merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db dlg-path merge {:form nil, :data nil, :view nil}
              options {:open? false})))

(rf/reg-event-fx
 ::submit
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [::subs/data]} _]
   {:dispatch-n (list
                 [:tta.component.settings.event/set-custom-emissivity data]
                 [::close])}))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db data-path data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db dlg-path merge options)))

(rf/reg-event-fx
 ::set-emissivity-field
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path value]]
   {:db (set-field-decimal db path value data
                           data-path form-path false
                           {:max 0.99, :min 0.01, :precision 2})}))

(rf/reg-event-db
 ::set-fill-all-field
 (fn [db [_ value]]
   (set-field-decimal db [:fill-all] value nil
                      nil form-path false
                      {:max 0.99, :min 0.01, :precision 2})))

(rf/reg-event-db
 ::set-level-index
 (fn [db [_ index]]
   (assoc-in db (conj dlg-path :view :selected-level-index) index)))

(rf/reg-event-fx
 ::fill-all
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/field [:fill-all]])
  (inject-cofx ::inject/sub [::subs/selected-level-index])]
 (fn [{:keys [db ::subs/data ::subs/field ::subs/selected-level-index]} _]
   (let [{:keys [value]} field
         value (js/Number value)]
     {:db
      (-> db
          (assoc-in data-path
                    (update-in data [:levels selected-level-index :tube-rows]
                               (fn [rows]
                                 (mapv
                                  (fn [tube-row]
                                    {:custom-emissivity
                                     (let [n (get-in tube-row [:custom-emissivity 0])]
                                       (vec (repeat 2 (vec (repeat (count n) value)))))})
                                  rows))))
          (assoc-in (conj form-path :levels) nil))})))

(rf/reg-event-fx
 ::clear-custom-emissivity
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/selected-level-index])]
 (fn [{:keys [db ::subs/data ::subs/selected-level-index]} [_ col-index]]
   {:db
    (-> db
     (assoc-in data-path
               (update-in data
                          [:levels selected-level-index :tube-rows
                           col-index :custom-emissivity]
                          (fn [custom-emissivity]
                            (let [n (count (first custom-emissivity))]
                              (vec (repeat 2 (vec (repeat n nil))))))))
     (assoc-in (conj form-path :levels selected-level-index
                     :tube-rows col-index)
               nil))}))
