(ns ht.user
  (:require [re-frame.core :as rf]
            [re-frame.db :as rd]
            [reagent.core :as r]
            [cljsjs.material-ui]
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as style]
            [ht.work.view :refer [work]]))

(defn workspace []
  [ui/mui-theme-provider
   {:mui-theme (get-mui-theme style/theme)}
   [work]])

(defn mount-div [id]
  (if-let [elm (js/document.getElementById id)]
    elm
    (let [elm (js/document.createElement "div")
          app (js/document.getElementById "app")]
      (.setAttribute elm "id" id)
      (js/document.body.insertBefore elm app)
      elm)))

(defn mount-workspace []
  (r/render [workspace] (mount-div "ht-user-workspace")))

(defn show-workspace
  ([]
   (mount-workspace)
   (rf/dispatch [:ht.work.event/open]))
  ([work-key]
   (mount-workspace)
   (rf/dispatch [:ht.work.event/open {:data {:work-key work-key}}])))
