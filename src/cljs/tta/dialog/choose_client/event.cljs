;; events for dialog choose-client
(ns tta.dialog.choose-client.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.util.service :as svc]
            [tta.entity :as e]
            [tta.dialog.choose-plant.event :as cp-event]))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db [:dialog :choose-client] merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db [:dialog :choose-client] merge options {:open? false})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value]]
   (assoc-in db [:dialog :choose-client :field id]
             {:valid? false
              :error nil
              :value value})))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :choose-client :data] data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :choose-client] merge options)))

(rf/reg-event-fx
 ::change-field
 (fn [{:keys [db]} [_ field value id]]
   {:dispatch-n (list [::set-field field value]
                      [::set-field :active-dispatch-id id])
    :dispatch-later [{:ms 1000
                      :dispatch [::search-clients id]}]}))
(rf/reg-event-fx
 ::search-clients
 (fn [{:keys [db]} [_ id]]
   (let [query {:name (get-in db
                              [:dialog :choose-client :field :name
                               :value])
                :country (get-in db
                                 [:dialog :choose-client :field :country
                                  :value])
                :shortName (get-in db
                                   [:dialog :choose-client :field :short-name
                                    :value])
                :location (get-in db
                                  [:dialog :choose-client :field :location
                                   :value])
                :havePlants (get-in db
                                    [:dialog :choose-client :field :have-plant :value
                                     ])}]
     (if (= (get-in db [:dialog :choose-client :field
                        :active-dispatch-id :value]) id)                         
       {:service/search-client
        {:query (into {} (remove (fn [[k v]] (nil? v)) query))
         :id id}}
       {}))))


(rf/reg-event-db
 ::set-client-list
 (fn [db [_ clients]]
   (assoc-in db [:dialog :choose-client :data :clients]
             (mapv (fn [cl]
                     (e/from-js :sap-client (clj->js cl)))
                   clients))))

(rf/reg-event-db
 ::select-client
 (fn [db [_ selected-client]]
   (assoc-in db [:dialog :choose-client :data :selected-client]
              selected-client)))

(rf/reg-event-fx
 ::set-active-client
 (fn [_ [_ client]]
   {:dispatch-n (list [::set-active-client-db client]
                      [::close]
                      [::cp-event/set-data {:fetched false
                                           }]
                      [::cp-event/open])}))

(rf/reg-event-db
 ::set-active-client-db
 (fn [db [_ client]]
   (-> db
       (assoc-in  [:client :active] (:id client))
       (assoc-in [:client :all (:id client)] client))))
