;; subscriptions for component home
(ns tta.component.home.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

(rf/reg-sub
 ::home
 (fn [db _] (get-in db [:component :home])))

(rf/reg-sub
 ::access-rules
 :<- [::ht-subs/auth-claims]
 :<- [::ht-subs/features]
 :<- [::ht-subs/operations]
 (fn [[claims features operations] _]
   ;;TODO: implement access rules based on claim
   ;; valid values -  :enabled :disabled :hidden
   {:card
    {:dataset-creator :enabled
     :dataset-analyzer :enabled
     :trendline :enabled
     :settings :enabled
     :config-history :enabled
     :goldcup :disabled
     :config :disabled
     :logs :enabled}
    :button
    {:data-entry :enabled
     :import-logger :enabled
     :print-logsheet :disabled}}))
