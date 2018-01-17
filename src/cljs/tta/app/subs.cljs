(ns tta.app.subs
  (:require [re-frame.core :as rf]))

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
   (get user :active)))

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
