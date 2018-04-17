;; events for component dataset-filter
(ns tta.component.dataset-filter.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

(rf/reg-event-db
  ::set-date-range
  (fn [db [_ range]]
    (assoc-in db [:component :dataset-filter :date-range] range)))
