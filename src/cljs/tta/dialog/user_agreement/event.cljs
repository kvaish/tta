;; events for dialog user-agreement
(ns tta.dialog.user-agreement.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [ht.app.subs :as ht-subs]
            [tta.app.event :as app-event]
            [tta.app.subs :as app-subs]))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db [:dialog :user-agreement] merge
              {:then nil} options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db [:dialog :user-agreement] merge options {:open? false})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value]]
   (assoc-in db [:dialog :user-agreement :field id]
             {:valid? false
              :error nil
              :value value})))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :user-agreement :data] data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :user-agreement] merge options)))

(rf/reg-event-fx
 ::set-agreed?
 (fn [{:keys [db]} [_ agreed?]]
   (let [user-id (:id @(rf/subscribe [::ht-subs/auth-claims]))
         user @(rf/subscribe [::app-subs/user])
         new? (nil? user)
         user (assoc user :id user-id, :agreed? agreed?)]
     {:service/save-user {:user user, :new? new?
                          :evt-success [::save-user-success user]
                          :evt-failure [::ht-event/service-failure true]}
      :dispatch [::ht-event/set-busy? true]})))

(rf/reg-event-fx
 ::save-user-success
 (fn [{:keys [db]} [_ {:keys [agreed?] :as user}]]
   (let [{:keys [on-accept on-decline]} (get-in db [:dialog :user-agreement :then])]
     {:dispatch-n (list [::app-event/set-user user]
                        [::ht-event/set-busy? false]
                        [::close]
                        (if agreed? on-accept on-decline))})))
