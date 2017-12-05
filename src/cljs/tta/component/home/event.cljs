;; events for component home
(ns tta.component.home.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [tta.app.event :as app-event]))

(rf/reg-event-db
 ::nav-data-entry
 (fn [db _]
   ;;TODO:
   db))

(rf/reg-event-db
 ::nav-import-logger
 (fn [db _]
   ;;TODO:
   db))

(rf/reg-event-db
 ::print-logsheet
 (fn [db _]
   ;;TODO:
   db))
