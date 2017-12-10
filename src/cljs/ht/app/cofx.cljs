(ns ht.app.cofx
  (:require [re-frame.core :as rf]
            [ht.util.common :as u]))

;; registry id policy: no namespace qualification on keyword

(rf/reg-cofx
 :window-size
 (fn [cofx _]
   (assoc cofx :window-size (u/get-window-size))))
