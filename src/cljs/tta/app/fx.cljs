(ns tta.app.fx
  (:require [re-frame.core :as rf]
            [ht.app.event :as ht-event]
            [tta.util.service :as svc]
            [tta.app.event :as event]))

(rf/reg-fx
 :service/fetch-user
 (fn [user-id]
   (svc/fetch-user-settings
    {:user-id user-id
     :evt-success [::event/set-user-settings user-id]
     :evt-failure [::ht-event/service-failure true]})))
