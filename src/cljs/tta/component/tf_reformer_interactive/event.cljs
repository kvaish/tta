;; events for component tf-reformer-interactive
(ns tta.component.tf-reformer-interactive.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

;; Add some event handlers, like
#_ (rf/reg-event-db
    ::event-id
    (fn [db [_ param]]
      (assoc db :param param)))
;;
;; NOTE: all event handler functions should be pure functions
;; Typically rf/reg-event-db should suffice for most cases, which
;; means you should not access or modify any global vars or make
;; external service calls.
;; If external data/changes needed use rf/reg-event-fx, in which case
;; your event handler function should take a co-effects map and return
;; a effects map, like
#_ (rf/reg-event-fx
    ::event-id
    (fn [{:keys[db]} [_ param]]
      {:db (assoc db :param param)}))
;;
;; If there is a need for external data then inject them using inject-cofx
;; and register your external data sourcing in cofx.cljs
;; Similarly, if your changes are not limited to the db, then use
;; rf/reg-event-fx and register your external changes as effects in fx.cljs
