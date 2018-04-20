(ns tta.app.fx
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [ht.app.event :as ht-event]
            [tta.util.service :as svc]
            [tta.app.event :as event]
            [tta.dialog.choose-client.event :as cc-event]))

(rf/reg-fx
 :service/fetch-user
 (fn [user-id]
   (svc/fetch-user
    {:user-id user-id
     :evt-success [::event/fetch-user-success]
     :evt-failure [::ht-event/service-failure true]})))

(rf/reg-fx
 :service/save-user
 (fn [{:keys [user new? evt-success evt-failure]}]
   (svc/save-user
    {:user user
     :new? new?
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-client
 (fn [client-id]
   (svc/fetch-client
    {:client-id client-id
     :evt-success [::event/fetch-client-success]
     :evt-failure [::ht-event/service-failure true]})))

(rf/reg-fx
 :service/search-clients
 (fn [{:keys [query evt-success evt-failure]}]
   (svc/search-clients
    {:query query
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-plant
 (fn [{:keys [client-id plant-id]}]
   (svc/fetch-plant
    {:client-id client-id
     :plant-id plant-id
     :evt-success [::event/fetch-plant-success]
     :evt-failure [::ht-event/service-failure true]})))

(rf/reg-fx
 :service/save-client
 (fn [{:keys [client new? evt-success evt-failure]}]
   (svc/save-client
    {:client client
     :new? new?
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/create-plant
 (fn [{:keys [client-id plant evt-success evt-failure]}]
   (svc/create-plant
    {:client-id client-id
     :plant plant
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/update-plant-config
 (fn [{:keys [client-id plant-id change-id config
             evt-success evt-failure]}]
   (svc/update-plant-config
    {:client-id client-id
     :plant-id plant-id
     :update-config {:change-id change-id
                     :config config}
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/update-plant-settings
 (fn [{:keys [client-id plant-id change-id settings
             evt-success evt-failure]}]
   (svc/update-plant-settings
    {:client-id client-id
     :plant-id plant-id
     :update-settings {:change-id change-id
                       :settings settings}
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-dataset
 (fn [{:keys [client-id plant-id dataset-id
             evt-success evt-failure]}]
   (svc/fetch-dataset
    {:client-id client-id
     :plant-id plant-id
     :dataset-id dataset-id
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/search-datasets
 (fn [{:keys [client-id plant-id from-date to-date
             evt-success evt-failure]}]
   (svc/search-datasets
    {:client-id client-id
     :plant-id plant-id
     :query {:utc-start from-date
             :utc-end to-date}
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/fetch-latest-dataset
 (fn [{:keys [client-id plant-id evt-success evt-failure]}]
   (svc/fetch-latest-dataset
    {:client-id client-id
     :plant-id plant-id
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))

(rf/reg-fx
 :service/create-dataset
 (fn [{:keys [dataset client-id plant-id evt-success evt-failure]}]
   (svc/create-dataset
    {:client-id client-id
     :plant-id plant-id
     :dataset dataset
     :evt-success evt-success
     :evt-failure (or evt-failure [::ht-event/service-failure false])})))
