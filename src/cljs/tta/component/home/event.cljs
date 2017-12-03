;; events for component home
(ns tta.component.home.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [tta.app.event :as app-event]))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:component :home :data] data)))

(rf/reg-event-fx
 ::send-data
 (fn [{:keys [db]} [_ data]]
   {:db (assoc db :busy? true)
    ;; raise more side effects, such as
    ;; :my-fx [my-data]
    }))