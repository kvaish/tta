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
                  [::load-client]
                  [::show-agreement])})))

(rf/reg-event-fx
 ::show-agreement
 (fn [{:keys [db]} _]
   (let [user-id (get-in db [:user :active])
         {:keys [agreed?]} (get-in db [:user :all user-id])]
     (js/console.log "show-agreement" agreed?)
     (if (nil? agreed?)
       {:dispatch [:tta.dialog.user-agreement.event/open]}))))

(rf/reg-event-fx
 ::update-user-settings
 (fn [_ [_ user-id]]
   {:service/fetch-user user-id}))


(rf/reg-event-fx
 ::load-client
 (fn [_ [_ user-id]]))
