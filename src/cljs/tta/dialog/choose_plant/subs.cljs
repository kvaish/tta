;; subscriptions for dialog choose-plant
(ns tta.dialog.choose-plant.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :as rr]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.service :as svc]
            [tta.dialog.choose-plant.event :as event]))

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :choose-plant])))


;;derived signals/subscriptions
(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))

(rf/reg-sub
 ::data
 :<- [::dialog]
 (fn [dialog _]
   (:data dialog)))

(rf/reg-sub
 ::field
 :<- [::dialog]
 (fn [dialog [_ id]]
   (get-in dialog [:field id])))

(rf/reg-sub-raw
 ::client
 (fn [dba _]
   (let [f #(get-in @dba [:dialog :choose-plant :data :client])
         client (f)]
     (if-not (:plants client)
       (svc/fetch-client
        {:client-id (:id client)
         :evt-success [::event/set-client]
         :evt-failure [::ht-event/service-failure false]}))
     (rr/make-reaction f))))

(rf/reg-sub-raw
 ::sap-plants
 (fn [dba _]
   (if @(rf/subscribe [::ht-subs/topsoe?])
     (let [f #(get-in @dba [:dialog :choose-plant :data :sap-plants])
           plants (f)]
       (if-not plants
         (svc/fetch-client-plants
          {:client-id (get-in @dba [:dialog :choose-plant :data :client :id])
           :evt-success [::event/set-sap-plants]
           :evt-failure [::ht-event/service-failure false]}))
       (rr/make-reaction f))
     (rr/make-reaction (constantly nil)))))

(rf/reg-sub
 ::plants
 :<- [::client]
 :<- [::sap-plants]
 :<- [::ht-subs/topsoe?]
 (fn [[client sap-plants topsoe?] _]
   (if-not topsoe?
     (mapv #(assoc % :config? true) (:plants client))
     (if sap-plants
       (mapv (fn [{:keys [id name capacity capacity-unit]}]
               {:id id, :name name
                :capacity (str capacity " " capacity-unit)
                :config? (some #(= id (:id %)) (:plants client))})
             sap-plants)))))
