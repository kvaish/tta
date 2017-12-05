;; subscriptions for component home
(ns tta.component.home.subs
  (:require [re-frame.core :as rf]
            [tta.util.auth :as auth]
            [tta.app.subs :as app-subs]))

(rf/reg-sub
 ::data
 (fn [db _]
   (get-in db [:component :home :data])))

(rf/reg-sub
 ::access-rules
 (fn [db _]
   ;;TODO: implement access rules based on claim
   ;; valid values -  enabled disabled hidden
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
