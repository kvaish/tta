(ns ht.core
  (:require [cljsjs.material-ui] ;; the first thing to ensure react loade
            [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.util.interop :as i]
            [ht.config :as config]
            [ht.setup :as setup]
            [ht.app.db :as db]
            [ht.app.cofx] ;; ensure load
            [ht.app.fx]   ;; ensure load
            [ht.app.event :as event]
            [ht.app.subs :as subs :refer [translate]]
            [ht.app.style :as style]
            [ht.app.view :refer [busy-screen]]))

(defn bind-resize-event []
  (i/oset js/window :onresize #(rf/dispatch [::event/update-view-size])))

(defn fetch-auth []
  (rf/dispatch [::event/fetch-auth]))

(defn init []
  (setup/init)
  (config/init)
  (db/init)
  (style/init)
  (bind-resize-event)
  (fetch-auth))

(defn init-db [app-db]
  (rf/dispatch-sync [::event/initialize-db (merge @db/default-db app-db)]))

(defn create-app [root]
  (fn []
    [ui/mui-theme-provider
     {:mui-theme (get-mui-theme style/theme)}
     [:div
      [root] ;;TODO:  sub claims  - has claims show root
      ;;TODO: auth fail component
      [busy-screen]]]))

(defn create-root-mounter [root]
  (let [app (create-app root)]
    (fn mount-root []
      (rf/clear-subscription-cache!)
      (r/render [app]
                (.getElementById js/document "app")))))
