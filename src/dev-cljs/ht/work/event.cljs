;; events for dialog work
(ns ht.work.event
  (:require [re-frame.core :as rf]))

(defonce dlg-path [:dialog :ht-workspace])

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db dlg-path merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db dlg-path merge options {:open? false})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value]]
   (assoc-in db (conj dlg-path :field id)
             {:valid? false
              :error nil
              :value value})))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db (conj dlg-path :data) data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db dlg-path merge options)))
