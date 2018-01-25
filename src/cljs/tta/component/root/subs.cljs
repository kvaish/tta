(ns tta.component.root.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

(rf/reg-sub
 ::root
 (fn [db _] (get-in db [:component :root])))

(rf/reg-sub
 ::active-content
 :<- [::root]
 (fn [root _]
   (get-in root [:content :active])))

(rf/reg-sub
 ::active-dataset-action
 (fn [db _]
   ;; TODO: implement to choose one of dataset-creator or dataset-analyzer
   :dataset-creator))

(rf/reg-sub
 ::app-allowed?
 :<- [::ht-subs/auth-claims]
 (fn [claims _]
   (auth/allow-app? claims)))

(rf/reg-sub
 ::content-allowed?
 :<- [::ht-subs/auth-claims]
 :<- [::active-content]
 (fn [[claims active-content] _]
   (auth/allow-root-content? claims active-content)))

(rf/reg-sub
 ::agreed?
 :<- [::app-subs/user]
 (fn [user _] (:agreed? user)))
