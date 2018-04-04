;; subscriptions for component tf-reformer-interactive
(ns tta.component.tf-reformer-interactive.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

;; primary signals
(rf/reg-sub
 ::tf-reformer-interactive
 (fn [db _]
   (get-in db [:component :tf-reformer-interactive])))

;; derived signals/subscriptions

(rf/reg-sub
  ::config
  :<- [::app-subs/plant]
  (fn [plant _]
    (:config plant)))
