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
         {:service/fetch-dataset
          (assoc fetch-params :dataset-id dataset-id)
          :db (assoc-in db (conj view-path :mode) :read)}

         (= mode :read)
         {:service/fetch-latest-dataset fetch-params
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

(rf/reg-event-db
 ::close
 (fn [db _] (assoc-in db comp-path nil)))

(rf/reg-event-fx
 ::fetch-success
 (fn [_ [_ dataset]]
   {:dispatch [::init {:dataset dataset}]}))

(rf/reg-event-fx
 ::fetch-failure
 (fn [_ [_ & params]]
   {:dispatch-n (list (into [::ht-event/service-failure false] params)
                      [:tta.component.root.event/activate-content :home])}))

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::excel
 (fn [_ _])
 ;;TODO: excel report
 )

(rf/reg-event-fx
 ::pdf
 (fn [_ _])
 ;;TODO: pdf report
 )

(rf/reg-event-fx
 ::save-draft
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/valid?])
  (inject-cofx ::inject/sub [::subs/dirty?])]
 (fn [{:keys [db ::subs/data ::subs/valid? ::subs/dirty?]} _]
   (if (and valid? dirty?) {:storage/set {:key :draft :value data}})))

(rf/reg-event-fx
 ::upload
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} _]
   ;;TODO: upload data
   (js/console.log "upload" data)))
