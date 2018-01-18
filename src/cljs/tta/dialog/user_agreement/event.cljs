;; events for dialog user-agreement
(ns tta.dialog.user-agreement.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
      (update-in db [:dialog :user-agreement] merge options {:open? true})))

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

(rf/reg-event-fx
 ::set-use-agreement
 (fn [_ [_ user-id data]]
   {:service/update-user {:user-id user-id :data data}}))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :user-agreement] merge options)))

