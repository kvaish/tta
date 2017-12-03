;; subscriptions for component home
(ns tta.component.home.subs
  (:require [re-frame.core :as rf]
            [tta.util.auth :as auth]
            [tta.app.subs :as app-subs]))

(rf/reg-sub
 ::data
 (fn [db _]
   (get-in db [:component :home :data])))