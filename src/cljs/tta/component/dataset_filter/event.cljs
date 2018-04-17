;; events for component dataset-selector
(ns tta.component.dataset-selector.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

(rf/reg-event-db
  ::set-date-range
  (fn [db [_ range]]
    (assoc-in db [:component :dataset-selector :date-range] range)))

(rf/reg-event-db
  ::set-dataset
  (fn [db [_ dataset]]
    (assoc-in db [:component :dataset-selector :dataset] dataset)))

