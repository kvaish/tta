;; subscriptions for component home
(ns tta.component.home.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]
            [ht.util.auth :as ht-auth]
            [tta.info :as info]
            [ht.util.common :as u]
            [tta.util.common :as au]))

(rf/reg-sub
 ::home
 (fn [db _]
   (get-in db [:component :home])))

(fn has-access [claims op]
  (if (ht-auth/allow-operation? claims op info/operations)
    :enabled
    :disabled))

(rf/reg-sub
 ::access-rules
 :<- [::ht-subs/auth-claims]
 ;; valid values -  :enabled :disabled :hidden
 (fn [claims _]
   (let [f (fn [[id pub?]]
             [id (if (or pub? (:topsoe? claims))
                   (if (auth/allow-root-content? claims id)
                     :enabled
                     :disabled)
                   :hidden)])]
     {:card
      (->>
       {:dataset        true
        :trendline      true
        :settings       true
        :gold-cup       false
        :config-history false
        :config         false
        :logs           true}
       (map f)
       (into {}))
      :button
      (->> {:data-entry     true
            :import-logger  true
            :print-logsheet true}
           (map f)
           (into {}))})))

(rf/reg-sub
 ::draft
 :<- [::home]
 :<- [::app-subs/plant]
 (fn [[{:keys [draft]} plant] _]
   (if (and (= (:plant-id draft) (:id plant))
            (= (:reformer-version draft)
               (get-in plant [:config :version])))
     draft)))

(rf/reg-sub
 ::has-draft?
 :<- [::draft]
 (fn [draft _] (some? draft)))
