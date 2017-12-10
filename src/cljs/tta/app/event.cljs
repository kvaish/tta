(ns tta.app.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]))


(rf/reg-event-fx
 ::fetch-user
 (fn [_ [_ user-id]]
   {:service/fetch-user user-id}))

;; ->fx, dispatch
;; show agreement if external and not agreed
;; choose client if internal and no last client
;; choose plant if no last plant
(rf/reg-event-db
 ::set-user-settings
 (fn [db [_ user-id settings]]
   (assoc-in db [:user :all user-id :settings] settings)))
