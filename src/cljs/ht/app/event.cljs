(ns ht.app.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [goog.date :as gdate]
            [ht.util.common :as u :refer [dev-log]]))

(rf/reg-event-db
 ::initialize-db
 (fn  [_ [_ app-db]]
   app-db))

(rf/reg-event-fx
 ::update-view-size
 [(inject-cofx :window-size)]
 (fn [{:keys [db window-size]} _]
   {:db (assoc-in db [:view-size] window-size)}))

(rf/reg-event-fx
 ::set-language
 (fn [{:keys [db]} [_ id]]
   (let [lang (u/pick-one #(= (name id) (:code %))
                          (get-in db [:config :languages]))]
     {:db (assoc-in db [:language :active] id)
      :storage/set-common {:key :language
                           :value lang}})))

(rf/reg-event-db
 ::set-busy?
 (fn [db [_ busy?]]
   (assoc-in db [:busy?] busy?)))

(rf/reg-event-fx
 ::exit
 (fn [_ _]
   {:app/exit nil}))

(rf/reg-event-fx
 ::logout
 (fn [_ _]
   {:db {:busy? true}
    :service/logout nil}))

(rf/reg-event-db
 ::service-failure
 (fn [db [_ fatal? res]]
   (assoc db :busy? false, :service-failure (assoc res :fatal? fatal?))))

(rf/reg-event-db
 ::show-message-box
 (fn [db [_ message-box]]
   ;; message-box - a map with following keys
   ;; message title level
   ;; label-ok event-ok
   ;; label-cancel event-cancel
   (assoc db :message-box (assoc message-box :open? true))))

(rf/reg-event-db
 ::close-message-box
 (fn [db _]
   (dissoc db :message-box)))

(rf/reg-event-fx
 ::fetch-auth
 (fn [_ [_ with-busy?]]
   (cond-> {:service/fetch-auth with-busy?}
     with-busy? (assoc :dispatch [::set-busy? true]))))

(rf/reg-event-fx
 ::set-auth
 (fn [{:keys [db]} [_ with-busy? token claims]]
   (let [now (.valueOf (gdate/DateTime.))
         delay (if claims (- (.valueOf (:exp claims)) now 300e3)) ;; 5min
         init? (and claims (not (get-in db [:auth :fetched?])))]
     (dev-log "fetch-auth after delay: " delay)
     (cond-> {:db (-> db
                      (update :auth assoc
                              :token token
                              :claims claims
                              :fetched? true))}
       delay (assoc :dispatch-later [{:ms delay
                                      :dispatch [::fetch-auth false]}])
       init? (update :dispatch-n conj
                     [:app/init]
                     [::fetch-translation]
                     [::fetch-app-roles])
       with-busy? (update :dispatch-n conj [::set-busy? false])))))

(rf/reg-event-fx
 ::fetch-translation
 (fn [_ _]
   {:service/fetch-translation {}}))

(rf/reg-event-fx
 ::set-translation
 (fn [{:keys [db]} [_ translation]]
   ;;TODO:
   ))

(rf/reg-event-fx
 ::fetch-app-roles
 (fn [_ _]
   {:service/fetch-app-roles {}}))

(rf/reg-event-db
 ::set-app-roles
 (fn [db [_ roles]]
   ;; structure of roles
   ;; {<role id>
   ;;  {:isInternal <true/false>,
   ;;   :about {<language id> {:name <name>,
   ;;                          :description <description>}
   ;;           ..}}
   ;;  ..}
   (assoc db :roles roles)))
