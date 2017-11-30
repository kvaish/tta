(ns tta.component.root.subs
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::active-menu-link
 (fn [db _]
   (get-in db [:view :home :menu-bar :active])))
