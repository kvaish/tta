;; events for component dataset
(ns tta.component.dataset.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.app.subs :as app-subs]
            [tta.component.dataset.subs :as subs]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-number
                                            set-field-temperature
                                            validate-field parse-value]]))

(def ^:const comp-path [:component :dataset])
(def ^:const view-path (conj comp-path :view))
(def ^:const data-path (conj comp-path :data))
(def ^:const form-path (conj comp-path :form))

;; Do NOT use rf/subscribe
;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]

(defn init-form [dataset]
  {:top-fired
   {:levels
    (reduce-kv (fn [m k lvl]
                 (assoc m k
                        {:rows
                         (mapv (fn [r]
                                 {:sides
                                  (mapv (fn [s]
                                          {:tubes (vec (repeat (count (:tubes s))
                                                               nil))})
                                        (:sides r))})
                               (:rows lvl))}))
               {}
               (get-in dataset [:top-fired :levels]))
    :wall-temps
    (reduce-kv (fn [m k temps]
                 (assoc m k
                        {:temps (vec (repeat (count (:temps temps)) nil))}))
               {}
               (get-in dataset [:top-fired :wall-temps]))
    :ceiling-temps
    (mapv (fn [temps]
            {:temps (vec (repeat (count (:temps temps)) nil))})
          (get-in dataset [:top-fired :ceiling-temps]))
    :floor-temps
    (mapv (fn [temps]
            {:temps (vec (repeat (count (:temps temps)) nil))})
          (get-in dataset [:top-fired :floor-temps]))}

   :side-fired
   {:chambers
    (mapv (fn [ch]
            {:sides
             (mapv (fn [s]
                     {:tubes (vec (repeat (count (:tubes s)) nil))
                      :wall-temps
                      (mapv (fn [pd]
                              {:temps (vec (repeat (count (:temps pd)) nil))})
                            (:wall-temps s))})
                   (:sides ch))})
          (get-in dataset [:side-fired :chambers]))}})

(rf/reg-event-fx
 ::init
 [(inject-cofx :storage :draft)
  (inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::app-subs/plant]
      {:keys [draft]} :storage}
     [_ {:keys [mode dataset dataset-id logger-data gold-cup?]}]]
   (let [draft (if (and
                    (= (:plant-id draft) (:id plant))
                    (= (:reformer-version draft) (get-in plant [:config :version])))
                 (au/dataset-from-storage draft))
         {:keys [client-id], plant-id :id} plant
         fetch-params {:client-id client-id
                       :plant-id plant-id
                       :evt-success [::fetch-success]
                       :evt-failure [::fetch-failure]}]
     (if dataset
       {:db (-> db
                (assoc-in (conj comp-path :dataset) dataset)
                (assoc-in form-path (init-form dataset)))}
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

;; VIEW STATE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

(rf/reg-event-db
 ::set-twt-entry-mode
 (fn [db [_ mode]]
   (assoc-in db (conj view-path :twt-entry-mode) mode)))

(rf/reg-event-db
 ::set-twt-entry-scope
 (fn [db [_ level-key scope]]
   (assoc-in db (conj view-path :twt-entry-scope level-key) scope)))

(rf/reg-event-db
 ::set-twt-entry-index
 (fn [db [_ level-key scope index]]
   (-> db
       (assoc-in (conj view-path :twt-entry-index scope) index)
       (assoc-in (conj view-path :twt-entry-scope level-key) scope))))

(rf/reg-event-fx
 ::move-twt-entry-index
 [(inject-cofx ::inject/sub (fn [[_ scope _]]
                              [::subs/twt-entry-index scope]))]
 (fn [{:keys [db ::subs/twt-entry-index]}
     [_ scope dir]] ;; dir = :prev, :next
   (let [index (if (= scope :wall)
                 (if (= dir :next)
                   (case twt-entry-index
                     :north :east
                     :east :south
                     :south :west
                     :north)
                   (case twt-entry-index
                     :north :west
                     :east :north
                     :south :east
                     :south))
                 ((if (= dir :next) inc dec) twt-entry-index))]
     {:db (assoc-in db (conj view-path :twt-entry-index scope) index)})))

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
 ::clear-draft
 (fn [{:keys [db]} _]
   (let [{:keys [gold-cup?]} (get-in db (conj comp-path :dataset))]
     {:storage/set {:key :draft, :value nil}
      :db (update-in db comp-path dissoc :dataset :data :form)
      :dispatch [:tta.dialog.dataset-settings.event/open
                 {:gold-cup? gold-cup?}]})))

(rf/reg-event-fx
 ::save-draft
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/valid?])
  (inject-cofx ::inject/sub [::subs/dirty?])]
 (fn [{:keys [::subs/data ::subs/valid? ::subs/dirty?]} _]
   (if (and valid? dirty?) {:storage/set {:key :draft, :value data}})))

(rf/reg-event-fx
 ::create-dataset-success
 (fn [_ [_ dataset-id]]
   {:dispatch [::init {:dataset-id (:new-id dataset-id)}]}))

(rf/reg-event-fx
 ::upload
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::app-subs/plant])
  (inject-cofx ::inject/sub [::app-subs/client])]
 (fn [{:keys [::subs/data ::app-subs/client ::app-subs/plant]} _]
   {:service/create-dataset {:client (:id client)
                             :plant-id (:id plant)
                             :dataset data
                             :evt-success [::create-dataset-success]}}))

;;Top fired TWT entry;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::set-temp
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::app-subs/temp-unit])]
 (fn [{:keys [db ::subs/data ::app-subs/temp-unit]}
     [_ path value required?]]
   {:db (set-field-temperature db path value data data-path form-path
                               required? temp-unit)}))

(rf/reg-event-fx
 ::clear-raw-temps
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ row-path]]
   (let [clean (fn [sides]
                 (mapv (fn [s]
                         (update s :tubes
                                 (fn [tubes]
                                   (mapv #(dissoc % :raw-temp) tubes))))
                       sides))]
     {:db (-> db
              (assoc-in data-path
                        (update-in data (conj row-path :sides) clean))
              (update-in (concat form-path row-path [:sides]) clean))})))

(rf/reg-event-fx
 ::add-temp-field
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path]]
   {:db (-> db
            (assoc-in data-path
                      (update-in data path
                                 #(conj (or % []) nil)))
            (update-in (concat form-path path)
                       #(conj (or % []) nil)))}))

(rf/reg-event-fx
 ::clear-wall-temps
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path]]
   (let [a (vec (repeat 5 nil))]
     {:db (-> db
              (assoc-in data-path
                        (assoc-in data path a))
              (assoc-in (concat form-path path) a))})))
