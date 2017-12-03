(ns tta.core
  (:require [cljsjs.material-ui] ;; the first thing to ensure react loade
            [reagent.core :as r]
            [re-frame.core :as rf]
            [tta.config]
            [tta.setup]
            [tta.app.core :refer [app]]))

(js/console.log "!VERSION!")

(defn mount-root []
  (rf/clear-subscription-cache!)
  (r/render [app]
            (.getElementById js/document "app")))

(defn ^:export init []
  (tta.setup/init)
  (tta.config/init)
  (tta.app.core/init)
  (mount-root))
