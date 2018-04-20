;; events for component dataset-selector
(ns tta.component.dataset-selector.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

(defonce comp-path [:component :dataset-selector])

(rf/reg-event-db
 ::set-filters
 (fn [db [_ filters]]
   (assoc-in db (conj comp-path :filters) filters)))

(rf/reg-event-fx
 ::select-dataset
 (fn [_ [_ dataset-id]]
   {:dispatch [:tta.component.root.event/activate-content
               :dataset {:dataset-id dataset-id}]}))

(rf/reg-event-fx
 ::fetch-datasets
 (fn [{:keys [db]} [_ plant date-range]]
   (let [plant-id   (:id plant)]
     {:db (assoc-in db (conj comp-path :fetching?) true)
      :service/search-datasets
      {:client-id   (:client-id plant)
       :plant-id    plant-id
       :from-date   (:start date-range)
       :to-date     (:end date-range)
       :evt-success [::fetch-datasets-success {:date-range date-range
                                               :plant-id   plant-id}]
       :evt-failure [::fetch-datasets-failure]}})))

(rf/reg-event-db
 ::fetch-datasets-success
 (fn [db [_ info datasets]]
   (update-in db comp-path
              #(-> %
                   (assoc :fetching? false)
                   (assoc :datasets datasets
                          :fetched-datasets-info info)))))

(rf/reg-event-fx
 ::fetch-datasets-failure
 (fn [{:keys [db]} [_ & params]]
   {:db (assoc-in db (conj comp-path :fetching?) false)
    :dispatch (into [::ht-event/service-failure false] params)}))
