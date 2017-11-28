(ns tta.app.views
  (:require [re-frame.core :as rf]
            [tta.app.subs :as subs]
            [tta.component.root.views :refer [root]]))

(defn app []
  [:div
   [root]])
