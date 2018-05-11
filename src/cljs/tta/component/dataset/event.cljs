;; events for component dataset
(ns tta.component.dataset.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-number
                                            set-field-temperature
                                            validate-field parse-value]]
            [tta.app.event :as app-event]
            [tta.app.subs :as app-subs]
            [tta.component.dataset.calc :refer [update-calc-summary
                                                ensure-view-factor
                                                apply-settings]]
            [tta.component.dataset.subs :as subs]))

(defonce comp-path [:component :dataset])
(defonce view-path (conj comp-path :view))
(defonce data-path (conj comp-path :data))
(defonce form-path (conj comp-path :form))

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
                                          {:tubes
                                           (vec (repeat (count (:tubes s))
                                                        nil))})
                                        (:sides r))})
                               (:rows lvl))
                         :wall-temps
                         (reduce-kv (fn [m k temps]
                                      (assoc m k
                                             {:temps
                                              (vec (repeat (count (:temps temps))
                                                           nil))}))
                                    {}
                                    (:wall-temps lvl))}))
               {}
               (get-in dataset [:top-fired :levels]))
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
 [(inject-cofx ::inject/sub [:tta.component.home.subs/draft])
  (inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::app-subs/plant :tta.component.home.subs/draft]}
     [_ {:keys [mode dataset dataset-id logger-data gold-cup?]}]]
   (if dataset
     ;; load the given dataset
     {:db (-> db
              (assoc-in (conj comp-path :dataset) dataset)
              (assoc-in data-path nil)
              (assoc-in form-path (init-form dataset)))
      :dispatch [::init-settings]}
     (let [{:keys [client-id], plant-id :id} plant
           fetch-params {:client-id client-id
                         :plant-id plant-id
                         :evt-success [::fetch-success]
                         :evt-failure [::fetch-failure]}]
       (cond
         dataset-id
         {:service/fetch-dataset (assoc fetch-params :dataset-id dataset-id)
          :db (update-in db view-path assoc
                         :mode :read, :fetching? true)}

         (= mode :read)
         {:service/fetch-latest-dataset fetch-params
          :db (update-in db view-path assoc
                         :mode :read, :fetching? true)}

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

         ;; not usual, while raising this event take care to add
         ;; right parameters so as to avoid one extra step
         ;; for better performance
         :default
         {:dispatch [::init {:mode (if draft :edit :read)}]})))))

(rf/reg-event-fx
 ::init-settings
 [(inject-cofx ::inject/sub [::subs/settings])
  (inject-cofx ::inject/sub [::subs/config])
  (inject-cofx ::inject/sub [::subs/mode])]
 (fn [{:keys [db ::subs/settings ::subs/config ::subs/mode]} _]
   ;; update settings in edit mode only
   (if (= :edit mode)
     {:db (update-in db (conj comp-path :dataset)
                    apply-settings settings config)})))

(rf/reg-event-db
 ::close
 (fn [db _] (assoc-in db comp-path nil)))

(rf/reg-event-fx
 ::fetch-success
 (fn [{:keys [db]} [_ dataset]]
   (if dataset
     {:dispatch [::init {:dataset dataset}]
      :db (assoc-in db (conj view-path :fetching?) false)}
     {:db (-> db
              (assoc-in (conj comp-path :dataset) nil)
              (assoc-in data-path nil)
              (assoc-in form-path nil)
              (assoc-in (conj view-path :fetching?) false ))})))

(rf/reg-event-fx
 ::fetch-failure
 (fn [_ [_ & params]]
   {:dispatch-n (list (into [::ht-event/service-failure false] params)
                      [:tta.component.root.event/activate-content :home])}))


;; VIEW STATE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::set-mode ;; optionally also enforces the area selection
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/config])
  (inject-cofx ::inject/sub [::subs/selected-area-id])]
 (fn [{:keys [db ::subs/data ::subs/config
             ::subs/selected-area-id]}
     [_ mode area-id]] ;; mode => :read or :edit
   {:db (cond-> (assoc-in db (conj view-path :mode) mode)
          ;; while switching to graph mode update calculations
          (= mode :read) (assoc-in data-path
                                   (update-calc-summary data config))
          ;; while switching to edit mode ensure view-factor.
          ;; this will happen only the first time you
          ;; switch to edit mode, hece data should be nil then.
          ;; update the src-data only instead of data.
          ;; otherwise it will result in a dirty state, but the user
          ;; has not made any changes yet!
          (= mode :edit) (update-in (conj comp-path :dataset)
                                   ensure-view-factor config))
    :dispatch [::select-area-by-id
               (or area-id
                   (if (and (= mode :read) (= selected-area-id :twt)
                            (= "top" (:firing config)))
                     ;; in case of top-fired, prefer overall
                     ;; when switching to graph from tube/wall entry
                     :overall
                     selected-area-id))]}))

(rf/reg-event-fx
 ::select-area-by-id
 (inject-cofx ::inject/sub [::subs/area-opts])
 (fn [{:keys [::subs/area-opts]} [_ area-id]]
   (let [area (some #(if (= (:id (second %)) area-id) (first %))
                    (map-indexed list area-opts))]
     {:dispatch [::select-area (or area 0)]})))

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

;; TWT-GRAPH ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-db
 ::set-twt-type
 (fn [db [_ value]]
   (assoc-in db (conj view-path :twt-type) value)))

(rf/reg-event-db
 ::set-reduced-firing?
 (fn [db [_ value]]
   (assoc-in db (conj view-path :reduced-firing?) value)))

(rf/reg-event-db
 ::set-avg-temp-band?
 (fn [db [_ value]]
   (assoc-in db (conj view-path :avg-temp-band?) value)))

(rf/reg-event-db
 ::set-avg-raw-temp?
 (fn [db [_ value]]
   (assoc-in db (conj view-path :avg-raw-temp?) value)))

(rf/reg-event-db
 ::set-avg-temp?
 (fn [db [_ value]]
   (assoc-in db (conj view-path :avg-temp?) value)))

;; BURNER-STATUS

(rf/reg-event-db
 ::set-burner-status-front-side
 (fn [db [_ ch-index side]]
   (assoc-in db (conj view-path :burner-status-front-side ch-index) side)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::datasheet
 (fn [_ _])
 ;;TODO: excel report
 )

(rf/reg-event-fx
 ::report
 (fn [_ _])
 ;;TODO: pdf report
 )

(rf/reg-event-fx
 ::reset-draft
 (fn [_ _]
   {:dispatch
    [::ht-event/show-message-box
     {:message (translate [:warning :reset-draft :message]
                          "The draft will be disacrded and a new one will be created!")
      :title (translate [:warning :reset-draft :title]
                        "Discard current data?")
      :level :warning
      :label-ok (translate [:action :discard :label] "Discard")
      :event-ok [::do-reset-draft]
      :label-cancel (translate [:action :cancel :label] "Cancel")}]}))

(rf/reg-event-fx
 ::do-reset-draft
 (fn [{:keys [db]} _]
   (let [{:keys [gold-cup?]} (get-in db (conj comp-path :dataset))]
     {:db (update-in db comp-path dissoc :dataset :data :form)
      :dispatch-n (list [:tta.dialog.dataset-settings.event/open
                         {:gold-cup? gold-cup?}]
                        [:tta.component.home.event/set-draft nil])})))

(rf/reg-event-fx
 ::save-draft
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/can-submit?])
  (inject-cofx ::inject/sub [::subs/config])]
 (fn [{:keys [db ::subs/data ::subs/can-submit? ::subs/config]} _]
   (if can-submit?
     (let [data (-> (update-calc-summary data config)
                    (assoc :last-saved (js/Date.)))]
       {:db (-> db
                (assoc-in (conj comp-path :dataset) data)
                (assoc-in data-path nil))
        :dispatch [:tta.component.home.event/set-draft data]}))))

;; TODO: need to enforce minimum measurement requirement
;; policy: <10: hidden, 10-50: red, 50-85: amber, >85 blue
(rf/reg-event-fx
 ::upload
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/settings])
  (inject-cofx ::inject/sub [::subs/config])
  (inject-cofx ::inject/sub [::subs/can-upload?])]
 (fn [{:keys [::subs/data ::subs/settings ::subs/config ::subs/can-upload?]} _]
   (if can-upload?
     ;; update calculations and upload
     (let [dataset (update-calc-summary data config)
           {:keys [tubes%]} (:summary dataset)
           {:keys [min-tubes%]} settings]
       (if (>= tubes% min-tubes%)
         ;; upload if enough tubes measured
         {:service/save-dataset {:dataset dataset
                                 :new? (:draft? dataset)
                                 :evt-success [::save-dataset-success]}}
         ;; warn if not enough
         {:dispatch
          [::ht-event/show-message-box
           {:message (translate [:warning :inadequate-dataset-upload :message]
                                "Please measure at least {min-tubes%} of tubes!"
                                {:min-tubes% (str min-tubes% "%")})
            :title (translate [:warning :inadequate-dataset-upload :title]
                              "Insufficient measurement!")
            :level :warning
            :label-ok (translate [:action :ok :label] "Ok")
            :event-ok [::set-mode :edit :twt]}]})))))

(rf/reg-event-fx
 ::save-dataset-success
 (fn [_ [_ {:keys [new-id]}]]
   {:dispatch-n (list
                 ;; TODO: re-fetch messages
                 ;; clear draft
                 [:tta.component.home.event/set-draft nil]
                 ;; re-fetch in read mode
                 [::init {:dataset-id new-id, :mode :read}])}))


;; burners ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::add-burners
 [(inject-cofx ::inject/sub [::subs/config])
  (inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/config ::subs/data]} _]
   (let [db
         (case (:firing config)
           "side"
           (let [{:keys [burner-row-count burner-count-per-row]}
                 (get-in config [:sf-config :chambers 0])
                 bs (vec (repeat burner-row-count
                                 (vec (repeat burner-count-per-row
                                              {:state "on"}))))]
             (assoc-in db data-path
                       (update-in data [:side-fired :chambers]
                                  (fn [chs]
                                    (mapv (fn [ch]
                                            (update ch :sides
                                                    (fn [sides]
                                                      (mapv (fn [s]
                                                              (assoc s :burners bs))
                                                            sides))))
                                          chs)))))

           "top"
           (let [{:keys [burner-rows]} (:tf-config config)]
             (assoc-in db data-path
                       (assoc-in data [:top-fired :burners]
                                 (mapv (fn [{:keys [burner-count]}]
                                         (vec (repeat burner-count
                                                      {:deg-open 90})))
                                       burner-rows)))))]
     {:db db})))

;; side-fired burners

(rf/reg-event-fx
 ::set-sf-burner
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ [ch-index side row col] value]]
   {:db (assoc-in db data-path
                  (assoc-in data [:side-fired :chambers ch-index :sides side
                                  :burners row col :state]
                            value))}))

;; top-fired burners

(rf/reg-event-fx
 ::set-tf-burner
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ row index value]]
   (if (<= 0 (js/Number value) 90)
     {:db (set-field-number db [:top-fired :burners row index :deg-open]
                            value data data-path form-path false
                            {:max 90, :min 0})})))

(rf/reg-event-db
  ::set-tf-burners-fill-all
  (fn [db [_ value]]
    (let [path (conj view-path :tf-burners :fill-all)]
      (if (empty? value)
        (assoc-in db path nil)
        (let [value (js/Number value)]
          (if (and (<= 0 value 90) (= value (js/Math.round value)))
            (assoc-in db path value)
            db))))))

(rf/reg-event-fx
  ::fill-all-tf-burners
  [(inject-cofx ::inject/sub [::subs/data])
   (inject-cofx ::inject/sub [::subs/config])]
  (fn [{:keys [db ::subs/data ::subs/config]} [_ value]]
    (let [{:keys [burner-rows]} (:tf-config config)]
      {:db (-> db
               (assoc-in data-path
                         (assoc-in data [:top-fired :burners]
                                   (mapv (fn [{:keys [burner-count]}]
                                           (vec (repeat burner-count
                                                        {:deg-open value})))
                                         burner-rows)))
               (assoc-in (conj form-path :top-fired :burners) nil))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
