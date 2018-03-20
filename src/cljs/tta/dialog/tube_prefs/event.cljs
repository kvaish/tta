;; events for dialog tube-prefs
(ns tta.dialog.tube-prefs.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.dialog.tube-prefs.subs :as subs]))

(def ^:const dlg-path [:dialog :tube-prefs])

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db [:dialog :tube-prefs] merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db dlg-path merge {:data nil}
              options {:open? false})))

(rf/reg-event-fx
 ::submit
 (fn [_  _]
   (let [data @(rf/subscribe [::subs/data])]
     {:dispatch-n (list
                   [:tta.component.settings.event/set-tube-prefs data]
                   [::close])})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ ch ind  value]]
   (let [tube-prefs @(rf/subscribe [:tta.dialog.tube-prefs.subs/data])
         new-data (assoc-in tube-prefs [ch ind] value)]
     (-> db
         (assoc-in [:dialog :tube-prefs :data] new-data)))))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :tube-prefs :data] data)))

(rf/reg-event-db
 ::clear-tube-prefs
 (fn [db [_ i]]
   (let [c (count (get-in db [:dialog :tube-prefs :data i]))] 
     (assoc-in db [:dialog :tube-prefs :data i]
               (vec (take c (repeat nil)))))))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :tube-prefs] merge options)))
