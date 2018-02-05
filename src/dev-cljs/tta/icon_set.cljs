(ns tta.icon-set
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.icon :as ic]))

(defn icon-set []
  (into [:div]
        (map (fn [[d i]]
               [:div {:style {:display "inline-block"
                              :width "100px"}}
                [i]
                [:span {:style {:display "block"}} d]])
             {"plant"   ic/plant
              "my-apps" ic/my-apps
              "logout"  ic/logout })))
