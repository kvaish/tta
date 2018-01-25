(ns tta.app.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.util.common :refer [dev-log]]
            [ht.app.event :as ht-event]
            [tta.util.auth :as auth]))

;;;;;;;;;;;;;;
;; app init ;;
;;;;;;;;;;;;;;

(rf/reg-event-fx
 :app/init
 (fn [{:keys [db]} _]
   (let [claims (get-in db [:auth :claims])]
     (if (auth/allow-app? claims)
       {:dispatch [::fetch-user (:id claims)]}))))

;;;;;;;;;;
;; user ;;
;;;;;;;;;;

(rf/reg-event-fx
 ::fetch-user
 (fn [_ [_ user-id]]
   (dev-log "fetch user: " user-id)
   {:service/fetch-user user-id
    :dispatch [::ht-event/set-busy? true]}))

(rf/reg-event-fx
 ::fetch-user-success
 (fn [_ [_ user]]
   {:dispatch-n (list [::set-user user]
                      [::ht-event/set-busy? false]
                      [::check-agreement])}))

(rf/reg-event-db
 ::set-user
 (fn [db [_ user]]
   (assoc db :user user)))

;;;;;;;;;;;;;;;
;; agreement ;;
;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::check-agreement
 (fn [{:keys [db]} _]
   {:dispatch (if (get-in db [:user :agreed?])
                [::load-client]
                [:tta.dialog.user-agreement.event/open
                 {:then {:on-accept [::load-client]
                         :on-decline [::ht-event/exit]}}])}))

;;;;;;;;;;;;
;; client ;;
;;;;;;;;;;;;

(rf/reg-event-fx
 ::load-client
 (fn [{:keys [db]} _]
   {:dispatch (if-let [client-id (or (get-in db [:auth :claims :client-id])
                                     (get-in db [:user :client-id]))]
                [::fetch-client client-id]
                [:tta.dialog.choose-client.event/open])}))

(rf/reg-event-fx
 ::fetch-client
 (fn [_ [_ client-id]]
   (dev-log "fetch-client:" client-id)
   {:service/fetch-client client-id
    :dispatch  [::ht-event/set-busy? true]}))

(rf/reg-event-fx
 ::fetch-client-success
 (fn [_ [_ client]]
   {:dispatch-n (list [::set-client client]
                      [::ht-event/set-busy? false]
                      [::load-plant])} ))

(rf/reg-event-db
 ::set-client
 (fn [db [_ client]]
   (assoc db :client client)))

;;;;;;;;;;;
;; plant ;;
;;;;;;;;;;;

(rf/reg-event-fx
 ::load-plant
 (fn [{:keys [db]} _]
   (let [plant-id (get-in db [:user :plant-id])
         client-id (get-in db [:client :id])]
     {:dispatch (if plant-id
                  [::fetch-plant client-id plant-id]
                  [:tta.dialog.choose-plant.event/open])})))

(rf/reg-event-fx
 ::fetch-plant
 (fn [_ [_ client-id plant-id]]
   (dev-log "fetch-plant:" client-id plant-id)
   {:service/fetch-plant {:client-id client-id
                          :plant-id plant-id}
    :dispatch [::ht-event/set-busy? true]}))

(rf/reg-event-fx
 ::fetch-plant-success
 (fn [_ [_ plant]]
   {:dispatch-n (list [::ht-event/set-busy? false]
                      [::set-plant plant])} ))

(rf/reg-event-db
 ::set-plant
 (fn [db [_ plant]]
   (assoc db :plant plant)))

;;;;;;;;;;;;;;;
;; countries ;;
;;;;;;;;;;;;;;;

(rf/reg-event-db
 ::set-search-options
 (fn [db [_ options]]
   (assoc db :countries (:country options))))
