(ns tta.app.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [tta.app.db :refer [default-db]]
            [tta.app.cofx]
            [tta.app.fx]))

(rf/reg-event-db
 ::initialize-db
 (fn  [_ _]
   default-db))

(rf/reg-event-fx
 ::update-view-size
 [(inject-cofx :window-size)]
 (fn [{:keys [db window-size]} _]
   {:db (assoc-in db [:view-size] window-size)}))

(rf/reg-event-db
 ::set-language
 (fn [db [_ id]]
   (assoc-in db [:language :active] id)))

(rf/reg-event-db
 ::set-busy?
 (fn [db [_ busy?]]
   (assoc-in db [:busy?] busy?)))

(rf/reg-event-db
 ::logout
 (fn [db _]
   ;;TODO:
   db))
