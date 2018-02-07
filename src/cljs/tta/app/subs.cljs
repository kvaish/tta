(ns tta.app.subs
  (:require [re-frame.core :as rf]
            [tta.util.service :as svc]
            [tta.app.event :as event]
            [ht.app.event :as ht-event]
            [reagent.ratom :as rr]))

;;;;;;;;;;;;;;;;;;;;;
;; Primary signals ;;
;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::user
 (fn [db _] (:user db)))

(rf/reg-sub
 ::client
 (fn [db _] (:client db)))

(rf/reg-sub
 ::plant
 (fn [db _] (:plant db)))

(rf/reg-sub-raw
 ::countries
 (fn [dba [_]]
   (let [f #(:countries @dba)]
     (if (empty? (f))
       (svc/fetch-search-options
        {:evt-success [::event/set-search-options]
         :evt-failure [::ht-event/service-failure true]}))
     (rr/make-reaction f))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Derived signals/subscriptions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::temp-unit
 :<- [::plant]
 (fn [plant _] (get-in plant [:settings :temp-unit])))
