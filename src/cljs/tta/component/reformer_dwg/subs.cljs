;; subscriptions for component reformer-dwg
(ns tta.component.reformer-dwg.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

;; primary signals
(rf/reg-sub
 ::reformer-dwg
 (fn [db _]
   (get-in db [:component :reformer-dwg])))

;; derived signals/subscriptions

(rf/reg-sub
 ::config
 :<- [::app-subs/plant]
 (fn [plant _]
   (:config plant)))
