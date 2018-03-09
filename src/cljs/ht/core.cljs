(ns ht.core
  (:require [cljsjs.material-ui] ;; the first thing to ensure react loaded
            [cljs-react-material-ui.core]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [ht.util.interop :as i]
            [ht.util.service :as svc]
            [ht.config :as config]
            [ht.setup :as setup]
            [ht.app.db :as db]
            [ht.app.cofx] ;; ensure load
            [ht.app.fx]   ;; ensure load
            [ht.app.event :as event]
            [ht.app.style :as style]
            [ht.app.view :refer [create-app]]))

(defn bind-resize-event []
  (i/oset js/window :onresize #(rf/dispatch [::event/update-view-size])))

(defn fetch-auth []
  (rf/dispatch [::event/fetch-auth :with-busy-screen]))

(defn init-db [app-db]
  (rf/dispatch-sync [::event/initialize-db (merge @db/default-db app-db)]))

(defn create-root-mounter [root]
  (let [app (create-app root)]
    (fn mount-root []
      (rf/clear-subscription-cache!)
      (r/render [app]
                (.getElementById js/document "app")))))

(defn init []
  (setup/init)
  (config/init)
  (db/init)
  (svc/init)
  (style/init)
  (bind-resize-event)
  (fetch-auth))
