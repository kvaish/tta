(ns tta.app.fx
  (:require [re-frame.core :as rf]
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
