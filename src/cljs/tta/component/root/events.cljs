(ns tta.component.root.events
  (:require [re-frame.core :as rf]))

(rf/reg-event-db
 ::set-active-menu-link
 (fn [db [_ menu-link]]
   (assoc-in db [:view :home :menu-bar :active] menu-link)))
