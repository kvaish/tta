(ns tta.app.core
  (:require [re-frame.core :as rf]
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.util.interop :as i]
            [tta.app.cofx]
            [tta.app.fx]
            [tta.app.db :as db]
            [tta.app.event :as event]
            [tta.app.subs :as subs]
            [tta.app.style :as style]
            [tta.component.root.view :refer [root]]))

(defn bind-resize-event []
  (i/oset js/window :onresize #(rf/dispatch [::event/update-view-size])))

(defn app []
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme style/theme)}
   [root]])

(defn init []
  (style/init)
  (rf/dispatch-sync [::event/initialize-db])
  (bind-resize-event))

