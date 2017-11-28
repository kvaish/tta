(ns tta.app.cofx
  (:require [re-frame.core :as rf]
            [tta.util.gen :as u]))

(rf/reg-cofx
 :window-size
 (fn [coeffects _]
   (assoc coeffects
          :window-size
          (u/get-window-size))))
