(ns ht.app.fx
  (:require [re-frame.core :as rf]
            [ht.util.common :as u]
            [ht.util.service :as svc]
            [ht.app.event :as event]))

(rf/reg-fx
 :service/fetch-auth
 (fn [_]
   (svc/fetch-auth {:evt-success [::event/set-auth]
                    :evt-failure [::event/service-failure true]}) ))


(rf/reg-fx
 :storage/set
 (fn [{:keys [key value]}]
   (u/set-storage key value)))

(rf/reg-fx
 :storage/set-common
 (fn [{:keys [key value]}]
   (u/set-storage key value true)))
