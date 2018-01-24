;; events for dialog choose-plant
(ns tta.dialog.choose-plant.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.entity :as e]))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db [:dialog :choose-plant] merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db [:dialog :choose-plant] merge options {:open? false})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value]]
   (assoc-in db [:dialog :choose-plant :field id]
             {:valid? false
              :error nil
              :value value})))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (js/console.log "Set-data" data)
   (assoc-in db [:dialog :choose-plant :data] data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :choose-plant] merge options)))

(rf/reg-event-fx
 ::change-client
 (fn [{:keys [db] } _]
   {:dispatch-n (list [::close]
                      [:tta.dialog.choose-client.event/open])}))
(rf/reg-event-fx
 ::set-plant-list
 (fn [{:keys [db] } [_ data]]
   {:dispatch [::set-data {:plant-list
                           (mapv (fn [pl]
                                   (e/from-js :sap-plant (clj->js pl)))
                                 data)
                           :fetched true}]}))

(rf/reg-event-fx
 ::select-plant
 (fn [{:keys [db]} [_ plant-id client-id]]
   {:service/update-user {:user-id (get-in db [:user :active])
                          :data {:plant-id plant-id
                                 :client-id client-id}}}))
