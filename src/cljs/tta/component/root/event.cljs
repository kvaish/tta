(ns tta.component.root.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.component.root.subs :as subs]))

(def all-contents
  {:dataset-creator {}
   :dataset-analyzer {}
   :trendline {}
   :config {:event/init [:tta.component.config.event/init]
            :subs/warn-on-close? [:tta.component.config.subs/warn-on-close?]
            :event/close [:tta.component.config.event/close]}
   :settings {:event/init [:tta.component.settings.event/init]
              :subs/warn-on-close? [:tta.component.settings.subs/warn-on-close?]
              :event/close [:tta.component.settings.event/close]}
   :gold-cup {}
   :config-history {}
   :logs {}})

(rf/reg-event-fx
 ::with-warning-for-unsaved-changes
 (fn [_ [_ next-event]]
   (let [warn? (as-> @(rf/subscribe [::subs/active-content]) $
                 (get-in all-contents [$ :subs/warn-on-close?])
                 (if $ @(rf/subscribe $)))]
     {:dispatch
      (if-not warn?
        next-event
        [::ht-event/show-message-box
         {:message (translate [:warning :unsaved :message]
                              "Unsaved changes will be lost!")
          :title (translate [:warning :unsaved :title]
                            "Discard current changes?")
          :level :warning
          :label-ok (translate [:action :discard :label] "Discard")
          :event-ok next-event
          :label-cancel (translate [:action :cancel :label] "Cancel")}])})))

(rf/reg-event-fx
 ::activate-content
 (fn [_ [_ id]]
   (let [cid @(rf/subscribe [::subs/active-content])]
     {:dispatch [::with-warning-for-unsaved-changes
                 [::close-and-init cid id]]})))

(rf/reg-event-fx
 ::close-and-init ;; close the current, init and open the next
 (fn [{:keys [db]} [_ close-id init-id]]
   (let [{:keys [event/close]} (get all-contents close-id)
         {:keys [event/init]} (get all-contents init-id)]
     {:db (assoc-in db [:component :root :content :active] init-id)
      :dispatch-n (list init close)})))

(rf/reg-event-db
  ::set-menu-open?
  (fn [db [_ id open?]]
    (assoc-in db [:component :root :menu id :open?] open?)))

(rf/reg-event-fx
 ::choose-plant
 (fn [_ _]
   {:dispatch [::with-warning-for-unsaved-changes [::choose-plant-continue]]}))

(rf/reg-event-fx
 ::choose-plant-continue
 (fn [_ _]
   (let [cid @(rf/subscribe [::subs/active-content])]
     {:dispatch-n (list [::close-and-init cid :home]
                        [:tta.dialog.choose-plant.event/open])})))

(rf/reg-event-fx
 ::logout
 (fn [_ _]
   {:dispatch [::with-warning-for-unsaved-changes [::ht-event/logout]]}))

(rf/reg-event-fx
 ::my-apps
 (fn [_ _]
   {:dispatch [::with-warning-for-unsaved-changes [::ht-event/exit]]}))
