;; events for component home
(ns tta.component.home.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

(rf/reg-event-fx
 ::show-license
 (fn [_ _]
   {:dispatch [:tta.dialog.user-agreement.event/open
               {:then {:on-decline [::ht-event/exit]}}]}))

(rf/reg-event-fx
 ::nav-data-entry
 (fn [_ _]
   {:dispatch [:tta.component.root.event/activate-content :dataset
               {:mode :edit}]}))

(rf/reg-event-db
 ::nav-import-logger
 (fn [db _]
   ;;TODO:
   (js/console.log "not implemented ::nav-import-logger")
   db))

(rf/reg-event-db
 ::print-logsheet
 (fn [db _]
   ;;TODO:
   (js/console.log "not implemented ::print-logsheet")
   db))

(rf/reg-event-fx
 ::nav-gold-cup
 (fn [_ _]
   {:dispatch [:tta.component.root.event/activate-content :dataset
               {:gold-cup? true}]}))
