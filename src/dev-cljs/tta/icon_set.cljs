(ns tta.icon-set
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [tta.app.icon :as ic]
            [ht.app.style :as ht-style]))

(defn icon-set []
  (into [:div]
        (map (fn [[d i full?]]
               [:div {:style {:display "inline-block"
                              :width "100px"}}
                [i {:style (if full? {:color (ht-style/color-hex :royal-blue)}
                               {:border-radius "50%"
                                :color (ht-style/color-hex :white)
                                :background (ht-style/color-hex :sky-blue)})}]
                [:span {:style {:display "block"}} d]])
             [["plant"   ic/plant true]
              ["my-apps" ic/my-apps true]
              ["logout"  ic/logout true]
              ["camera"  ic/camera]
              ["plus" ic/plus]
              ["minus" ic/minus]
              ["dropdown" ic/dropdown]
              ["nav-left" ic/nav-left]
              ["nav-right" ic/nav-right]
              ["pyrometer+" ic/pyrometer+]
              ["emissivity+" ic/emissivity+]
              ["mark-tube" ic/mark-tube]])))
