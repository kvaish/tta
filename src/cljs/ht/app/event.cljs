(ns ht.app.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [goog.string :as gstr]
            [goog.string.format]
            [goog.date :as gdate]))

(rf/reg-event-db
 ::initialize-db
 (fn  [_ [_ app-db]]
   app-db))

(rf/reg-event-fx
 ::update-view-size
 [(inject-cofx :window-size)]
 (fn [{:keys [db window-size]} _]
   {:db (assoc-in db [:view-size] window-size)}))

(rf/reg-event-db
 ::set-language
 (fn [db [_ id]]
   (assoc-in db [:language :active] id)))

(rf/reg-event-db
 ::set-busy?
 (fn [db [_ busy?]]
   (assoc-in db [:busy?] busy?)))

(rf/reg-event-db
 ::logout
 (fn [db _]
   ;;TODO:
   db))

(rf/reg-event-fx
 ::service-failure
 (fn [_ [_ fatal? status]]
   {:dispatch [:dialog/open :service-failure {:status status
                                              :fatal? fatal?}]}))

(rf/reg-event-fx
 ::fetch-auth
 (fn [_ _]
   {:service/fetch-auth nil}))

(defn set-auth [{:keys [db]} [_ token claims]]
  (let [now (.valueOf (gdate/DateTime.))
        claims (if claims (update claims :exp gdate/fromIsoString))
        delay (if claims (- (.valueOf (:exp claims)) now 300e3)) ;; 5min
        fetch-user? (and claims (not (get-in db [:user :active])))
        user-id (:id claims)]
    (cond-> {:db (-> db
                     (update :auth assoc :token token :claims claims)
                     (assoc-in [:user :active] user-id))}
      delay (assoc :disptach-later [{:ms delay
                                     :dispatch [:fetch-auth]}])
      fetch-user? (assoc :dispatch [:fetch-user user-id]))))

(rf/reg-event-fx ::set-auth set-auth)

