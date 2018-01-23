(ns tta.app.fx
  (:require [re-frame.core :as rf]
            [ht.app.event :as ht-event]
            [tta.util.service :as svc]
            [tta.app.event :as event]
            [tta.dialog.choose-client.event :as cc-event]))

(rf/reg-fx
 :service/fetch-user
 (fn [user-id]
   (svc/fetch-user-settings
    {:user-id user-id
     :evt-success [::event/fetch-user-success user-id]
     :evt-failure [::ht-event/service-failure true]})))

(rf/reg-fx
 :service/update-user
 (fn [data]
   (svc/update-user-settings
    {:user-id (:user-id data)
     :data (:data data)
     :evt-success [::event/update-user-settings (:user-id data)]
     :evt-failure [::ht-event/service-failure true]})))


(rf/reg-fx
 :service/create-user
 (fn [data]
   (svc/create-user
    {:user-id (:user-id data)
     :data (:data data)
     :evt-success [::event/update-user-settings (:user-id data)]
     :evt-failure [::ht-event/service-failure true]})))


(rf/reg-fx
 :service/fetch-client-search-options
 (fn [_]
   (svc/fetch-search-options
    {:evt-success [::event/set-client-search-options]
     :evt-failure [::ht-event/service-failure true]})))


(rf/reg-fx
 :service/search-client
 (fn [data id]
   (svc/search-clients
    {:query  (:query data)
     :evt-success [::cc-event/set-client-list]
     :evt-failure [::ht-event/service-failure true]})))

(rf/reg-fx
 :service/fetch-plant
 (fn [{:keys [client-id plant-id]}]
   (svc/fetch-plant
    {:client-id client-id
     :plant-id plant-id
     :evt-success [::event/fetch-plant-success]
     :evt-failure [::ht-event/service-failure true]})))

(rf/reg-fx
 :service/fetch-client
 (fn [{:keys [client-id]}]
   (svc/fetch-plant
    {:client-id  client-id
     :evt-success [::event/fetch-client-success]
     :evt-failure [::ht-event/service-failure true]})))
