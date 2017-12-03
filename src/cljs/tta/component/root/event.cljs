(ns tta.component.root.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [tta.app.event :as app-event]))

(rf/reg-event-db
 ::activate-content
 (fn [db [_ id]]
   (assoc-in db [:component :root :content :active] id)))

(rf/reg-event-db
 ::show-language-menu
 (fn [db _]
   ;;TODO:
   db))

(rf/reg-event-db
 ::show-settings-menu
 (fn [db _]
   ;;TODO:
   db))
