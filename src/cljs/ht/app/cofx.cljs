(ns ht.app.cofx
  (:require [re-frame.core :as rf]
            [ht.util.common :as u]))

;; registry id policy: no namespace qualification on keyword

(rf/reg-cofx
 :window-size
 (fn [cofx _]
   (assoc cofx :window-size (u/get-window-size))))

(rf/reg-cofx
 :storage
 (fn [cofx key]
   (assoc-in cofx [:storage key] (u/get-storage key))))

(rf/reg-cofx
 :storage-common ;; when common between multiple apps
 (fn [cofx key]
   (assoc-in cofx [:storage key] (u/get-storage key true))))
