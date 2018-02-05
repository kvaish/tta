;; subscriptions for component reformer-layout
(ns tta.component.reformer-layout.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

;; primary signals
(rf/reg-sub
 ::reformer-layout
 (fn [db _]
   (get-in db [:component :reformer-layout])))

;; derived signals/subscriptions
