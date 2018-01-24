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

(rf/reg-sub
 ::dataset
 (fn [db _] (:dataset db)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Derived signals/subscriptions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::active-user
 :<- [::user]
 (fn [user _]
   (get-in user [:all (:active user)])))

(rf/reg-sub
 ::active-client
 :<- [::client]
 (fn [client _]
   (get-in client [:all (:active client)])))

(rf/reg-sub
 ::active-plant
 :<- [::plant]
 (fn [plant _]
   (get-in plant [:all (:active plant)])))

(rf/reg-sub
 ::active-dataset
 :<- [::dataset]
 (fn [dataset _]
   (get-in dataset [:all (:active dataset)])))

(rf/reg-sub-raw
 ::country-list
 (fn [dba [_]]
   (let [countries (get @dba :countries)]
     (if (or (empty? countries) (nil? countries))
       (svc/fetch-search-options
        {:evt-success [::event/set-client-search-options]
         :evt-failure [::ht-event/service-failure true]})))
   (rr/make-reaction
    (fn [] (get @dba :countries)))))
