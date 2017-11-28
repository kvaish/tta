(ns tta.core
  (:require [cljsjs.material-ui] ;; the first thing to ensure react loaded
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [reagent.core :as r]
            [re-frame.core :as rf]
            [tta.config]
            [tta.setup]
            [tta.app.style :refer [theme]]
            [tta.app.events :as events]
            [tta.app.views :as v]
            [tta.util.interop :as i]))

(js/console.log "!VERSION!")

(defn app []
  [ui/mui-theme-provider {:mui-theme (get-mui-theme theme)}
   [v/app]])

(defn mount-root []
  (rf/clear-subscription-cache!)
  (r/render [app]
            (.getElementById js/document "app")))

(defn bind-resize-event []
  (i/oset js/window :onresize #(rf/dispatch [::events/update-view-size])))

(defn ^:export init []
  (tta.setup/init)
  (tta.config/init)
  (tta.app.style/init)
  (rf/dispatch-sync [::events/initialize-db])
  (bind-resize-event)
  (mount-root))
