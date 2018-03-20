;; events for dialog choose-plant
(ns tta.dialog.choose-plant.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

(rf/reg-event-fx
 ::open
 [(inject-cofx ::inject/sub [:tta.app.subs/client])]
 (fn [{:keys [db tta.app.subs/client]} [_ options]]
   (let [options (update-in options [:data :client] #(or % client))]
     {:db (update-in db [:dialog :choose-plant] merge options {:open? true})})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db [:dialog :choose-plant] merge options {:open? false})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value]]
   (assoc-in db [:dialog :choose-plant :field id] {:valid? false
                                                   :error nil
                                                   :value value})))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :choose-plant :data] data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :choose-plant] merge options)))

(rf/reg-event-fx
 ::change-client
 (fn [_ _]
   {:dispatch-n (list [::close]
                      [:tta.dialog.choose-client.event/open])}))

(rf/reg-event-db
 ::set-client
 (fn [db [_ client]]
   (assoc-in db [:dialog :choose-plant :data :client]
             (update client :plants #(or % [])))))

(rf/reg-event-db
 ::set-sap-plants
 (fn [db [_ sap-plants]]
   (assoc-in db [:dialog :choose-plant :data :sap-plants] sap-plants)))

(rf/reg-event-fx
 ::select-plant
 [(inject-cofx ::inject/sub [:ht.app.subs/auth-claims])
  (inject-cofx ::inject/sub [:tta.app.subs/user])]
 (fn [{:keys [db ht.app.subs/auth-claims tta.app.subs/user]} [_ plant]]
   (let [client (get-in db [:dialog :choose-plant :data :client])
         {:keys [id]} auth-claims
         new? (nil? user)
         user (assoc user :id id
                     :client-id (:id client)
                     :plant-id (:id plant))]
     {:dispatch-n (list [::close]
                        [::app-event/set-client client]
                        [::app-event/fetch-plant (:id client) (:id plant)])
      :service/save-user {:user user, :new? new?
                          :evt-success [::app-event/set-user user]
                          :evt-failure [::ht-event/service-failure false]}})))

(rf/reg-event-fx
 ::configure-plant
 (fn [_ [_ plant]]
   {:dispatch [::select-plant plant]
    :forward-events {:register ::configure
                     :events #{:tta.app.event/fetch-plant-success
                               :ht.app.event/service-failure}
                     :dispatch-to [::open-configure-plant]}}))

(rf/reg-event-fx
 ::open-configure-plant
 (fn [_ _]
   {:dispatch-n (list [:tta.component.root.event/activate-content :config])
    :forward-events {:unregister ::configure}}))
