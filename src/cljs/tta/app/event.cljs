(ns tta.app.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [tta.entity :refer [from-js]]
            [tta.schema.user :as us]
            [ht.util.common :as u]
            [ht.app.event :as ht-event]
            [tta.entity :as e]))

(rf/reg-event-fx
 :app/init
 (fn [{:keys [db]} _]
   {:dispatch-n (list [::ht-event/set-busy? true]
                      [::fetch-user (get-in db [:user :active])])}))

(rf/reg-event-fx
 ::fetch-user
 (fn [_ [_ user-id]]
   {:service/fetch-user user-id}))

(rf/reg-event-fx
 ::fetch-user-success
 (fn [_ [_ user-id user]]
   {:dispatch-n (list [::ht-event/set-busy? false]
                      [::set-user user-id user])}))


;; ->fx, dispatch
;; show agreement if external and not agreed
;; choose client if internal and no last client
;; choose plant if no last plant
(rf/reg-event-fx
 ::set-user
 (fn [{:keys [db]} [_ user-id user]]
   {:db (assoc-in db  [:user :all user-id] (e/from-js :user (clj->js user)))
    :dispatch [::check-agreement]}))

(rf/reg-event-fx
 ::check-agreement
 (fn [{:keys [db]} _]
   (let [user-id (get-in db [:user :active])
         {:keys [agreed?]} (get-in db [:user :all user-id])]
     {:dispatch (if agreed?
                  [::load-client user-id]
                  [::show-agreement])})))

(rf/reg-event-fx
 ::show-agreement
 (fn [{:keys [db]} _]
   (let [user-id (get-in db [:user :active])
         {:keys [agreed?]} (get-in db [:user :all user-id])]

     {:dispatch (if (nil? agreed?)
                  [:tta.dialog.user-agreement.event/open]
                  [:tta.dialog.user-agreement.event/close])})))

(rf/reg-event-fx
 ::update-user-settings
 (fn [_ [_ user-id]]
   {:dispatch-n (list 
                 [::fetch-user user-id])}))


(rf/reg-event-fx
 ::load-client
 (fn [{:keys [db]} [_ user-id]]
   (let [client-id
         (get-in db [:user :all user-id :client-id])]
     (if client-id
       {:dispatch-n (list [::ht-event/set-busy? true]
                          [::fetch-client client-id])}
       {:dispatch  [::select-client user-id client-id]}))))

(rf/reg-event-fx
 ::fetch-client
 (fn [_ [_ client-id]]
   {:service/fetch-client {:client-id client-id}}))


(rf/reg-event-fx
 ::fetch-client-success
 (fn [_ [_ client]]
   {:dispatch-n (list
                 [::ht-event/set-busy? false]
                 [::set-client client]
                 [::load-plant])} ))

(rf/reg-event-fx
 ::select-client
 (fn [{:keys [db]} [_ user-id client-id]]
   (let [claims (get-in db [:auth :claims])
         selected-client (get-in db [:client :active])]
     (if (and (:isTopsoe claims) (not selected-client))
       {:dispatch [:tta.dialog.choose-client.event/open]}
       {}))))

(rf/reg-event-fx
 ::load-plant
 (fn [{:keys [db]} [_]]
   (let [user-id (get-in db [:user :active])
         plant-id (get-in db [:user :all user-id :plant-id])
         client-id (get-in db [:user :all user-id :client-id])]
     
     (if plant-id
       {:dispatch-n (list [::ht-event/set-busy? true]
                          [::fetch-plant client-id plant-id])}
       {:dispatch [:tta.dialog.choose-plant.event/open]}) )))


(rf/reg-event-fx
 ::fetch-plant
 (fn [_ [_ client-id plant-id]]
   {:service/fetch-plant {:client-id client-id
                          :plant-id plant-id}} ))


(rf/reg-event-fx
 ::fetch-plant-success
 (fn [_ [_ plant]]
   {:dispatch-n (list
                 [::ht-event/set-busy? false]
                 [::set-plant plant])} ))

(rf/reg-event-db
 ::set-plant
 (fn [db [_ data]]
   (-> db
       (assoc-in [:plant :active] (:id data))
       (assoc-in [:plant :all (:id data)] data))))

(rf/reg-event-db
 ::set-client
 (fn [db [_ data]]
   (-> db
       (assoc-in [:client :active] (:id data))
       (assoc-in [:client :all (:id data)] data))))

(rf/reg-event-db
 ::set-client-search-options
 (fn [db [_ data]]
   (assoc-in db [:countries] (get data :country))))
