;; subscriptions for component reformer-layout-tf
(ns tta.component.reformer-layout-tf.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

;; primary signals
(rf/reg-sub
 ::reformer-layout-tf
 (fn [db _]
   (get-in db [:component :reformer-layout-tf])))

;; derived signals/subscriptions
