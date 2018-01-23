(ns tta.component.root.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

(rf/reg-sub
 ::root
 (fn [db _] (get-in db [:component :root])))

(rf/reg-sub
  ::open?
  :<- [::dialog]
  (fn [dialog _]
    (:open? dialog)))

(rf/reg-sub
  ::setting-disable?
  :<- [::root]
  (fn [root _]
    (if (or (= (get-in root [:content :active]) :dataset-analyzer)
            (= (get-in root [ :content :active]) :trendline))
      (:setting-disable? false)
      )))

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
 ::content-allowed?
 :<- [::ht-subs/auth-claims]
 :<- [::active-content]
 (fn [[claims active-content] _]
   (auth/allow-root-content? claims active-content)))

(rf/reg-sub
 ::agreed?
 :<- [::app-subs/user]
 (fn [user db]
   (let [user-id (get user :active)
         users (get user :all)]
          (:agreed? (get users user-id)))))

(rf/reg-sub
  ::languages
  :<- [::ht-subs/language-options]
  :<- [::ht-subs/active-language]
  (fn [[opt active-lang] _]
 (merge {} {:options opt :active active-lang})
))

(rf/reg-sub
  ::active-setting
  (fn [db _]
    (get-in db [:settings :active])
    ))




