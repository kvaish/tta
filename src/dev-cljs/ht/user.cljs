(ns ht.user
  (:require [re-frame.core :as rf]
            [re-frame.db :as rd]
            [reagent.core :as r]))

(defn show-workspace
  ([]
   (rf/dispatch [:ht.work.event/open]))
  ([work-key]
   (rf/dispatch [:ht.work.event/open {:data {:work-key work-key}}])))
