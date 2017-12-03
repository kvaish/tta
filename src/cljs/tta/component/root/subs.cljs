(ns tta.component.root.subs
  (:require [re-frame.core :as rf]
            [tta.util.auth :as auth]
            [tta.app.subs :as app-subs]))

(rf/reg-sub
 ::active-content
 (fn [db _]
   (get-in db [:component :root :content :active])))

(rf/reg-sub
 ::active-dataset-action
 (fn [db _]
   ;; TODO: implement to choose one of dataset-creator or dataset-analyzer
   :dataset-creator))

(rf/reg-sub
 ::content-allowed?
 :<- [::app-subs/auth-claims]
 :<- [::active-content]
 (fn [[claims active-content] _]
   (auth/allow-root-content? claims active-content)))
