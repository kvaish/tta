(ns tta.component.root.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

(rf/reg-event-db
  ::activate-content
  (fn [db [_ id]]
    (js/console.log id)
    (assoc-in db [:component :root :content :active] id)))


(rf/reg-event-db
  ::set-menu-open?
  (fn [db [_ id open?]]
    (assoc-in db [:component :root :menu id :open?] open?)))
