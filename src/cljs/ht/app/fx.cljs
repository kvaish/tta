(ns ht.app.fx
  (:require [re-frame.core :as rf]
            [ht.util.common :as u]
            [ht.util.service :as svc]
            [ht.util.interop :as i]
            [ht.config :refer [config]]
            [ht.app.event :as event]))

(rf/reg-fx
 :app/exit
 (fn [_]
   (i/oset js/htAppEnv :leaveSilently true)
   (let [{:keys [portal-uri]} @config]
     (i/oset js/window.location :href portal-uri))))

;;;;;;;;;;;;;
;; storage ;;
;;;;;;;;;;;;;

(rf/reg-fx
 :storage/set
 (fn [{:keys [key value]}]
   (u/set-storage key value)))

(rf/reg-fx
 :storage/set-common
 (fn [{:keys [key value]}]
   (u/set-storage key value true)))

;;;;;;;;;;;;;
;; service ;;
;;;;;;;;;;;;;

(rf/reg-fx
 :service/fetch-auth
 (fn [with-busy?]
   (svc/fetch-auth {:evt-success [::event/set-auth with-busy?]
                    :evt-failure [::event/set-auth with-busy? nil nil]})))

(rf/reg-fx
 :service/logout
 (fn [_]
   (svc/logout {:evt-success [::event/exit]
                :evt-failure [::event/exit]})))
