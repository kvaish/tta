(ns tta.app.events
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [tta.db :as db]
            [tta.app.cofx]))

(rf/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/default-db))

(rf/reg-event-fx
 ::update-view-size
 [(inject-cofx :window-size)]
 (fn [{:keys [db window-size]} _]
   {:db (assoc-in db [:view :size] window-size)}))

(rf/reg-event-db
 ::set-language
 (fn [db [_ id]]
   (assoc-in db [:language :active] id)))
