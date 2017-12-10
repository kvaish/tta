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

(defn init [app-db]
  (setup/init)
  (config/init)
  (style/init)
  (rf/dispatch-sync [::event/initialize-db (merge db/default-db app-db)])
  (bind-resize-event))

(defn create-app [root]
  (fn []
    [ui/mui-theme-provider
     {:mui-theme (get-mui-theme style/theme)}
     [:div
      [root]
      [busy-screen]]]))

(defn create-root-mounter [root]
  (let [app (create-app root)]
    (fn mount-root []
      (rf/clear-subscription-cache!)
      (r/render [app]
                (.getElementById js/document "app")))))
