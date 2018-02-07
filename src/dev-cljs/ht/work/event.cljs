;; events for dialog work
(ns ht.work.event
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db [:dialog :work] merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db [:dialog :work] merge options {:open? false})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value]]
   (assoc-in db [:dialog :work :field id]
             {:valid? false
              :error nil
              :value value})))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :work :data] data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :work] merge options)))
