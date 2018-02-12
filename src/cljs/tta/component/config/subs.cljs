;; subscriptions for component config
(ns tta.component.config.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :as rr]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

;; primary signals
(rf/reg-sub
  ::config
  (fn [db _]
    (:config db)))

(rf/reg-sub
 ::component
 (fn [db _]
   (get-in db [:component :config])))

(rf/reg-sub
  ::get-field
  (fn [db [_ path]]
    (get-in db path)))

(rf/reg-sub
  ::set-field
  (fn [db [_ path value]]
    (js/console.log "called")
    (assoc-in db path value)))

;; derived signals/subscriptions
