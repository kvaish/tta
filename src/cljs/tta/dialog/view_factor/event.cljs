;; events for dialog view-factor
(ns tta.dialog.view-factor.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [tta.dialog.view-factor.subs :as subs]
            [tta.app.event :as app-event]
            [tta.util.common :refer [set-field-decimal]]))

;; Do NOT use rf/subscribe
;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]

(defonce ^:const dlg-path [:dialog :view-factor])
(defonce ^:const data-path (conj dlg-path :data))
(defonce ^:const form-path (conj dlg-path :form))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db dlg-path merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db dlg-path merge  {:form nil, :data nil, :view nil}
              options {:open? false})))

(rf/reg-event-fx
 ::submit
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [::subs/data]} _]
   {:dispatch-n (list
                 [:tta.component.config.event/set-view-factor data]
                 [::close])}))

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value]]
   (assoc-in db [:dialog :view-factor :field id]
             {:valid? false
              :error nil
              :value value})))

(rf/reg-event-fx
 ::set-view-factor-field
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_  level key row side index value]]
   (print [level :tube-rows row key side index])
   {:db (set-field-decimal db [level :tube-rows row key side index]
                           value data
                           data-path form-path false
                           {:max 0.99, :min 0.01, :precision 2})}))

(rf/reg-event-db
 ::set-level
 (fn [db [_ index]]
   (assoc-in db (conj dlg-path :view :selected-level) index)))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :view-factor :data] data)))

(rf/reg-event-db
 ::set-wall-fill-all-field
 (fn [db [_ value]]
   (set-field-decimal db [:fill-all-wall] value nil
                      nil form-path false
                      {:max 0.99, :min 0.01, :precision 2})))

(rf/reg-event-db
 ::set-ceiling-fill-all-field
 (fn [db [_ value]]
   (set-field-decimal db [:fill-all-ceiling] value nil
                      nil form-path false
                      {:max 0.99, :min 0.01, :precision 2})))

(rf/reg-event-db
 ::set-floor-fill-all-field
 (fn [db [_ value]]
   (set-field-decimal db [:fill-all-floor] value nil
                      nil form-path false
                      {:max 0.99, :min 0.01, :precision 2})))


(rf/reg-event-db
 ::set-row-selection
 (fn [db [_ value]]
   (assoc-in db (conj form-path  :row-selection) value)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :view-factor] merge options)))

(rf/reg-event-fx
 ::clear-row
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/form])
  (inject-cofx ::inject/sub [::subs/selected-level-key])]
 (fn [{:keys [db ::subs/data ::subs/form ::subs/selected-level-key]}
     [_ row]]
   {:db (-> db
            (assoc-in data-path (assoc-in data [selected-level-key :tube-rows row] nil))
            (assoc-in form-path (assoc-in form [selected-level-key :tube-rows row] nil)))})) 

(rf/reg-event-fx
 ::fill-all
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/form])
  (inject-cofx ::inject/sub [::subs/selected-level-key])
  (inject-cofx ::inject/sub [::subs/config])
  (inject-cofx ::inject/sub [::subs/row-selection])]
 (fn [ {:keys [db ::subs/data ::subs/form
              ::subs/fill-all-wall
              ::subs/selected-level-key ::subs/config ::subs/row-selection]}
     [_ up-key value]]
   
   (let [tube-row-count (get-in config [:tf-config :tube-row-count])
         tube-rows (get-in config [:tf-config :tube-rows])
         value (js/Number value)]
     {:db
      (-> db       
          (assoc-in data-path
                    (update-in data [selected-level-key :tube-rows]
                               (fn [rows]
                                 (mapv (fn [row {:keys [tube-count]} i]
                                         (if (or (= -1 row-selection)
                                                 (= row-selection i) )
                                           (assoc row up-key
                                                  (vec
                                                   (repeat 2 (vec
                                                              (repeat
                                                               tube-count
                                                               value))))) row))
                                       rows tube-rows (range)))) )
          (assoc-in form-path 
                    (update-in form [selected-level-key :tube-rows]
                               (fn [rows]
                                 (mapv 
                                  (fn [row]
                                    (if (and (vector? row) (contains? row up-key)) 
                                      (assoc (last row) up-key nil))
                                    (if (contains? row up-key)(assoc row up-key nil)))
                                  rows)))))})))

