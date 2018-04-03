;; events for component dataset
(ns tta.component.dataset.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.app.subs :as app-subs]
            [tta.component.dataset.subs :as subs]))

(def ^:const comp-path [:component :dataset])
(def ^:const view-path (conj comp-path :view))
(def ^:const data-path (conj comp-path :data))
(def ^:const form-path (conj comp-path :form))

;; Do NOT use rf/subscribe
;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]

(rf/reg-event-fx
 ::init
 [(inject-cofx :storage :draft)
  (inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::app-subs/plant]
      {:keys [draft]} :storage}
     [_ {:keys [mode dataset dataset-id logger-data gold-cup?]}]]
   (let [draft (if (= (:reformer-version draft)
                      (get-in plant [:config :version]))
                 draft)
         {:keys [client-id], plant-id :id} plant
         fetch-params {:client-id client-id
                       :plant-id plant-id
                       :evt-success [::fetch-success]
                       :evt-failure [::fetch-failure]}]
     (if dataset
       {:db (assoc-in db (conj comp-path :dataset) dataset)}
       (cond
         dataset-id
         {:dispatch [:service/fetch-dataset
                     (assoc fetch-params :dataset-id dataset-id)]
          :db (assoc-in db (conj view-path :mode) :read)}

         (= mode :read)
         {:dispatch [:service/fetch-latest-dataset fetch-params]
          :db (assoc-in db (conj view-path :mode) :read)}

         logger-data
         {:dispatch [:tta.dialog.dataset-settings.event/open
                     {:logger-data logger-data}]
          :db (assoc-in db (conj view-path :mode) :edit)}

         (or gold-cup? (= mode :edit))
         (-> {:db (assoc-in db (conj view-path :mode) :edit)}
             (assoc :dispatch
              (if draft
                [::init {:dataset (cond-> draft
                                    gold-cup? (assoc :gold-cup? true))}]
                [:tta.dialog.dataset-settings.event/open
                 {:gold-cup? gold-cup?}])))

         :default
         {:dispatch [::init {:mode (if draft :edit :read)}]})))))

(rf/reg-event-fx
 ::close
 (fn [{:keys [db]} _]
   ))

(rf/reg-event-fx
 ::fetch-success
 (fn []))

(rf/reg-event-fx
 ::fetch-failure
 (fn []))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
 ::set-mode
 (fn [db [_ mode]] ;; mode => :read or :edit
   (assoc-in db (conj view-path :mode) mode)))

(rf/reg-event-db
 ::select-area
 (fn [db [_ area]]
   (assoc-in db (conj view-path :selected-area) area)))

(rf/reg-event-db
 ::select-level
 (fn [db [_ level]]
   (assoc-in db (conj view-path :selected-level) level)))
