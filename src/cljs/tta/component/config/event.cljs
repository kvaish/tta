;; events for component config
(ns tta.component.config.event
  (:require [clojure.string :as str]
            [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-number
                                            validate-field parse-value]]
            [tta.app.event :as app-event]
            [tta.component.config.subs :as subs]
            [tta.app.subs :as app-subs]))

(defonce ^:const comp-path [:component :config])
(defonce ^:const data-path (conj comp-path :data))
(defonce ^:const form-path (conj comp-path :form))

(def chk? (some-fn nil? :valid?))

(rf/reg-event-fx
 ::set-data-field ;; used for initializing a property with default value
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path value]]
   {:db (assoc-in db data-path (assoc-in data path value))}))

(rf/reg-event-fx
 ::init
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} _]
   {:forward-events (list
                     ;; top-fired
                     {:register ::tf-burner-row
                      :events #{::set-tf-burner-first?
                                ::set-tf-tube-row-count}
                      :dispatch-to [::set-tf-burner-row-count]}
                     {:register ::tf-measure-levels-validity
                      :events #{::set-tf-measure-level?}
                      :dispatch-to [::validate-tf-measure-levels]}
                     ;; side-fired
                     {:register ::sf-peep-doors
                      :events #{::set-sf-tube-count
                                ::set-sf-pd-count
                                ::set-sf-section-count}
                      :dispatch-to [::set-sf-peep-doors]}
                     {:register ::sf-chamber-validity
                      :events #{::set-sf-peep-doors
                                ::set-sf-burner-count-per-row
                                ::set-sf-pd-tube-count}
                      :dispatch-to [::validate-sf-chamber]})
    :db (-> db
            (assoc-in data-path (update data :name #(or % "Reformer")))
            (assoc-in (conj form-path :firing)
                      (if-not (:firing data) (missing-field))))}))

(rf/reg-event-fx
 ::close
 (fn [{:keys [db]} _]
   {:db (assoc-in db comp-path nil)
    :forward-events (list {:unregister ::tf-burner-row}
                          {:unregister ::tf-measure-levels-validity}
                          {:unregister ::sf-peep-doors}
                          {:unregister ::sf-chamber-validity})}))

(defn bump-version? [config old-config]
  (if old-config
    (if (not= (:firing config) (:firing old-config)) ;; firing changed - unlikely
      true
      (case (:firing config)
        "side" ;; side-fired
        (let [chs  (get-in config [:sf-config :chambers])
              ochs (get-in old-config [:sf-config :chambers])]
          (or
           (not= (count chs) (count ochs))
           (some (fn [cs]
                   (apply not= (map #(select-keys % [:peep-door-tube-count
                                                     :peep-door-count
                                                     :tube-count
                                                     :burner-count-per-row
                                                     :burner-row-count])
                                    cs)))
                 (map list chs ochs))))
        "top"  ;; top-fired
        (let [tf (:tf-config config)
              otf (:tf-config old-config)]
          (or
           (let [ks [:tube-row-count :burner-row-count :measure-levels]]
             (not= (select-keys tf ks) (select-keys otf ks)))
           (some (fn [[ck rk]]
                   (not= (map ck (rk tf)) (map ck (rk otf))))
                 [[:tube-count :tube-rows]
                  [:burner-count :burner-rows]])))
        ;;unlikely
        true))))

(rf/reg-event-fx
 ::update-client-plants
 (fn [_ [_ client plant]]
   (let [new-client? (empty? (:plants client))
         client (update client :plants
                        conj {:id (:id plant)
                              :name (:name plant)
                              :capacity (str/join " " [(:capacity plant)
                                                       (:capacity-unit plant)])})]
     {:service/save-client
      {:client client
       :new? new-client?
       :evt-success [::app-event/fetch-client (:id client)]}})))

(rf/reg-event-fx
 ::sync-after-save
 [(inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::app-subs/plant]} [_ [eid]]]
   (cond-> {:forward-events {:unregister ::sync-after-save}}
     (= eid ::app-event/fetch-plant-success)
     (assoc :db (assoc-in db data-path (:config plant))))))

(rf/reg-event-fx
 ::do-upload
 [(inject-cofx ::inject/sub [::app-subs/client])]
 (fn [{:keys [::app-subs/client]} [_ config plant]]
   (merge {:forward-events {:register ::sync-after-save
                            :events #{::app-event/fetch-plant-success
                                      ::ht-event/service-failure}
                            :dispatch-to [::sync-after-save]}}
          (if (:config plant)
            ;; existing plant, update config => fetch-plant
            {:dispatch [::ht-event/set-busy? true]
             :service/update-plant-config
             {:client-id (:id client), :plant-id (:id plant)
              :change-id (:change-id plant)
              :config config
              :evt-success [::app-event/fetch-plant (:id client) (:id plant)]}}
            ;; new plant, create plant => save-client
            (let [{:keys [firing sf-config tf-config]} config
                  settings (case firing
                             "top" {:tf-settings
                                    {:tube-rows
                                     (mapv (fn [{tc :tube-count}]
                                             {:tube-prefs (vec (repeat tc nil))})
                                           (:tube-rows tf-config))}}
                             "side" {:sf-settings
                                     {:chambers
                                      (mapv (fn [{tc :tube-count}]
                                              {:tube-prefs (vec (repeat tc nil))})
                                            (:chambers sf-config))}})]
              {:dispatch [::ht-event/set-busy? true]
               :service/create-plant
               {:client-id (:id client)
                :plant (assoc plant :config config, :settings settings)
                :evt-success [::update-client-plants client plant]}})))))

(rf/reg-event-fx
 ::upload
 [(inject-cofx ::inject/sub [::subs/can-submit?])
  (inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::subs/can-submit? ::subs/data ::app-subs/plant]} _]
   (merge (when can-submit?
            {:dispatch
             (if (bump-version? data (:config plant))
               ;; warn and then upload
               [::ht-event/show-message-box
                {:message (translate [:config :critical-change :message]
                                     "You are going to make critical changes to configruation. Existing datasets will no longer be compatible. You can view them but not edit!")
                 :title (translate [:config :critical-change :title]
                                   "Major configuration change!")
                 :level :warning
                 :label-ok (translate [:action :continue :label] "Continue")
                 :event-ok [::do-upload data plant]
                 :label-cancel (translate [:action :cancel :label] "Cancel")}]
               ;; no need to war. directly upload
               [::do-upload data plant])})
          {:db (update-in db comp-path assoc :show-error? true)})))

(rf/reg-event-fx
 ::set-field
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path value required?]]
   {:db (set-field db path value data data-path form-path required?)}))

(rf/reg-event-fx
 ::set-text
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path value required?]]
   {:db (set-field-text db path value data data-path form-path required?)}))

(rf/reg-event-fx
 ::set-number
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path value required? {:keys [max min]}]]
   {:db (set-field-number db path value data data-path form-path required?
                          {:max max, :min min})}))

(defn distribute
  "return a list of numbers of length **p** and whose sum is **n**"
  [n p]
  (let [q (quot n p)
        r (rem n p)
        e (quot r 2)]
    (if (zero? r)
      (repeat p q)
      (concat (repeat (- r e) (inc q))
              (repeat (- p r) q)
              (repeat e (inc q))))))

;;; COMMON ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-reformer-name [name]
  (rf/dispatch [::set-text [:name] name true]))

(rf/reg-event-fx
 ::set-firing
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ firing]]
   {:db (cond-> db
          :data (assoc-in data-path
                          (assoc data :firing firing))
          :form (assoc-in (conj form-path :firing) nil)
          (= firing "top")
          (update-in form-path
                     (fn [form]
                       (assoc form :sf-config nil
                              :tf-config
                              {:tube-rows [{:tube-count (missing-field)}]
                               :burner-rows [{:burner-count (missing-field)}]
                                        ;:section-count (missing-field)
                               :tube-row-count (missing-field)})))
          (= firing "side")
          (update-in form-path
                     (fn [form]
                       (assoc form :tf-config nil
                              :sf-config
                              {:chambers
                               [(reduce #(assoc %1 %2 (missing-field)) {}
                                        [:section-count :peep-door-count
                                         :tube-count :burner-count-per-row
                                         :burner-row-count])]})))
          ;; init defaults
          (= firing "top")
          (update-in data-path
                     (fn [data]
                       (assoc data :sf-config nil
                              :tf-config {:burner-first? false
                                          :measure-levels {:top? true
                                                           :middle? false
                                                           :bottom? false}
                                          :wall-names {:north "North"
                                                       :east "East"
                                                       :west "West"
                                                       :south "South"}})))
          (= firing "side")
          (update-in data-path
                     (fn [data]
                       (assoc data :tf-config nil
                              :sf-config {:placement-of-WHS "end"
                                          :dual-nozzle? false
                                          :chambers [{:name "Chamber"
                                                      :side-names ["A" "B"]}]}))))}))

;;; TOP-FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn set-tf-wall-name [wall-key wall-name]
  (rf/dispatch [::set-text [:tf-config :wall-names wall-key] wall-name true]))

(rf/reg-event-fx
 ::set-tf-wall-name
 (fn [_ [_ wall-key wall-name]]
   (set-tf-wall-name wall-key wall-name)))

(rf/reg-event-fx
 ::set-tf-measure-level?
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ level-key measure?]]
   {:db (assoc-in db data-path
                  (assoc-in data [:tf-config :measure-levels level-key] measure?))}))

(rf/reg-event-fx
 ::validate-tf-measure-levels
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} _]
   (let [{:keys [top? bottom? middle?]} (get-in data [:tf-config :measure-levels])]
     {:db (assoc-in db (conj form-path :tf-config :measure-levels-validity)
                    (if-not (or top? bottom? middle?)
                      {:error "at least one level must be selected!"
                       :valid? false}))})))

(rf/reg-event-fx
 ::set-tf-burner-first?
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ first?]]
   {:db (assoc-in db data-path (assoc-in data [:tf-config :burner-first?] first?))}))

(defn reset-tf-rows [db row-type]
  (let [[row-count-key rows-key count-key start-key end-key sel-key name-key]
        (case row-type
          :tube [:tube-row-count :tube-rows
                 :tube-count :start-tube :end-tube
                 :tube-numbers-selection :name]
          :burner [:burner-row-count :burner-rows
                   :burner-count :start-burner :end-burner
                   :burner-numbers-selection])
        row-count (get-in db (conj data-path :tf-config row-count-key))
        count-per-row (get-in db (conj data-path :tf-config rows-key 0 count-key))]
    (-> db
        (assoc-in (conj data-path :tf-config rows-key)
                  (reduce #(conj %1 (assoc %2 name-key
                                           (str "Row " (inc (count %1)))))
                          []
                          (repeat row-count {count-key count-per-row
                                             start-key 1, end-key count-per-row,
                                             })))
        (assoc-in (conj form-path :tf-config rows-key)
                  (vec (repeat row-count {sel-key "00"
                                          count-key (if-not count-per-row
                                                      (missing-field))}))))))

(defn set-tf-row-count
  "**row-type**: either :tube or :burner,
  update the row count as well as update the rows vector to match the count."
  [db data row-type row-count]
  (let [[row-count-key checks] (case row-type
                                 :tube [:tube-row-count {:min 2, :max 9}]
                                 :burner [:burner-row-count nil])
        db (set-field-number db [:tf-config row-count-key] (str row-count)
                             data data-path form-path true checks)
        valid? (chk? (get-in db (conj form-path :tf-config row-count-key)))]
    (cond-> db
      valid? (reset-tf-rows row-type))))

(rf/reg-event-fx
 ::set-tf-tube-row-count
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ row-count]]
   {:db (set-tf-row-count db data :tube row-count)}))

(rf/reg-event-fx
 ::set-tf-burner-row-count
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} _]
   (let [{:keys [burner-first? tube-row-count]} (:tf-config data)
         valid? (chk? (get-in db (conj form-path :tf-config :tube-row-count)))]
     (if valid?
       (let [row-count (if burner-first? (inc tube-row-count) (dec tube-row-count))]
         {:db (set-tf-row-count db data :burner row-count)})))))

(defn set-tf-count-per-row
  "set count as well as reset start=1 and end=count in each row."
  [db data row-type count-per-row]
  (let [[rows-key count-key] (case row-type
                               :tube [:tube-rows :tube-count]
                               :burner [:burner-rows :burner-count])
        db (set-field-number db [:tf-config rows-key 0 count-key]
                             count-per-row data data-path form-path true)
        valid? (chk? (get-in db (conj form-path :tf-config rows-key 0 count-key)))]
    (cond-> db
      valid? (reset-tf-rows row-type))))

(rf/reg-event-fx
 ::set-tf-tube-count-per-row
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ tube-count]]
   {:db (set-tf-count-per-row db data :tube tube-count)}))

(rf/reg-event-fx
 ::set-tf-burner-count-per-row
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ burner-count]]
   {:db (set-tf-count-per-row db data :burner burner-count)}))

(defn set-tf-numbers-selection
  [db data row-type row-index sel-id numbers-options]
  (let [[start end] (some #(if (= (:id %) sel-id) (:nums %)) numbers-options)
        [rows-key start-key end-key sel-key]
        (case row-type
          :tube [:tube-rows :start-tube :end-tube :tube-numbers-selection]
          :burner [:burner-rows :start-burner :end-burner :burner-numbers-selection])]
    (-> db
        (assoc-in data-path
                  (update-in data [:tf-config rows-key row-index]
                             assoc start-key start, end-key end))
        (assoc-in (conj form-path :tf-config rows-key row-index sel-key) sel-id))))

(rf/reg-event-fx
 ::set-tf-tube-numbers-selection
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/tf-tube-numbers-options])]
 (fn [{:keys [db ::subs/data ::subs/tf-tube-numbers-options]}
     [_ row-index sel-id]]
   {:db (set-tf-numbers-selection db data :tube
                                  row-index sel-id tf-tube-numbers-options)}))

(rf/reg-event-fx
 ::set-tf-burner-numbers-selection
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/tf-burner-numbers-options])]
 (fn [{:keys [db ::subs/data ::subs/tf-burner-numbers-options]}
     [_ row-index sel-id]]
   {:db (set-tf-numbers-selection db data :burner
                                  row-index sel-id tf-burner-numbers-options)}))

;;; SIDE-FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 ::set-sf-placement-of-WHS
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ whs]]
   {:db (assoc-in db data-path
                  (assoc-in data [:sf-config :placement-of-WHS] whs))}))

(rf/reg-event-fx
 ::set-sf-dual-nozzle?
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ dual?]]
   {:db (assoc-in db data-path
                  (assoc-in data [:sf-config :dual-nozzle?] dual?))}))

(rf/reg-event-fx
 ::set-sf-dual-chamber?
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ dual?]]
   (let [ch (get-in data [:sf-config :chambers 0])
         {:keys [tube-count burner-count-per-row]} ch
         chs (->> (if dual? [ch ch] [ch])
                  (mapv #(assoc % :start-tube 1, :end-tube tube-count
                                :start-burner 1, :end-burner burner-count-per-row)))
         fch (get-in db (conj form-path :sf-config :chambers 0))
         fchs (->> (if dual? [fch (select-keys fch [:name :side-names])] [fch])
                   (mapv #(assoc % :tube-numbers-selection "00"
                                 :burner-numbers-selection "00")))]
     {:db (-> db
              (assoc-in data-path
                        (-> data
                            (assoc-in [:sf-config :chambers] chs)
                            (update-in [:sf-config :placement-of-WHS]
                                       #(if dual? "end" %))))
              (assoc-in (conj form-path :sf-config :chambers) fchs))})))

(defn set-sf-chamber-name [ch-index name]
  (rf/dispatch [::set-text [:sf-config :chambers ch-index :name] name true]))

(defn set-sf-side-name [ch-index side-index name]
  (rf/dispatch [::set-text [:sf-config :chambers ch-index :side-names side-index]
                name true]))

(defn update-sf-chamber-2 [db check-key data-keys form-keys]
  (let [valid? (and (= 2 (count (get-in db (conj data-path :sf-config :chambers))))
                    (or (nil? check-key)
                        (get-in db (conj form-path :sf-config :chambers 0
                                         check-key :valid?))))
        data-keys (not-empty (remove nil? (conj data-keys check-key)))]
    (cond-> db
      (and valid? data-keys)
      (update-in (conj data-path :sf-config :chambers)
                 #(update-in % [1] merge (select-keys (first %) data-keys)))
      (and valid? form-keys)
      (update-in (conj form-path :sf-config :chambers)
                 #(update-in % [1] merge (select-keys (first %) form-keys))))))

(defn reset-sf-numbers [db sel-type]
  (let [[count-key start-key end-key sel-key]
        (case sel-type
          :tube [:tube-count :start-tube :end-tube :tube-numbers-selection]
          :burner [:burner-count-per-row :start-burner :end-burner
                   :burner-numbers-selection])
        valid? (chk? (get-in db (conj form-path :sf-config :chambers 0 count-key)))]
    (cond-> db
      valid? (update-in (conj data-path :sf-config :chambers 0)
                        #(assoc % start-key 1, end-key (get % count-key)))
      valid? (assoc-in (conj form-path :sf-config :chambers 0 sel-key) "00"))))

(rf/reg-event-fx
 ::set-sf-section-count
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ section-count]]
   {:db (-> (set-field-number db [:sf-config :chambers 0 :section-count]
                              section-count data data-path form-path true {:min 1})
            (update-sf-chamber-2 :section-count nil nil))}))

(rf/reg-event-fx
 ::set-sf-pd-count
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ pd-count]]
   {:db (-> (set-field-number db [:sf-config :chambers 0 :peep-door-count]
                              pd-count data data-path form-path true {:min 1})
            (update-sf-chamber-2 :peep-door-count nil nil))}))

(rf/reg-event-fx
 ::set-sf-tube-count
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ tube-count]]
   {:db (-> (set-field-number db [:sf-config :chambers 0 :tube-count]
                              tube-count data data-path form-path true {:min 1})
            (reset-sf-numbers :tube)
            (update-sf-chamber-2 :tube-count
                                 [:start-tube :end-tube]
                                 [:tube-numbers-selection]))}))

(rf/reg-event-fx
 ::set-sf-burner-count-per-row
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ burner-count]]
   {:db (-> (set-field-number db [:sf-config :chambers 0 :burner-count-per-row]
                              burner-count data data-path form-path true {:min 1})
            (reset-sf-numbers :burner)
            (update-sf-chamber-2 :burner-count-per-row
                                 [:start-burner :end-burner]
                                 [:burner-numbers-selection]))}))

(rf/reg-event-fx
 ::set-sf-burner-row-count
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ row-count]]
   {:db (-> (set-field-number db [:sf-config :chambers 0 :burner-row-count]
                              row-count data data-path form-path true {:min 1})
            (update-sf-chamber-2 :burner-row-count nil nil))}))

(rf/reg-event-fx
 ::set-sf-pd-tube-count
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ pd-index tube-count]]
   {:db (-> (set-field-number db [:sf-config :chambers 0 :peep-door-tube-count pd-index]
                              tube-count data data-path form-path true {:min 1})
            (update-sf-chamber-2 nil [:peep-door-tube-count] nil))}))

(rf/reg-event-fx
 ::set-sf-peep-doors
 (fn [{:keys [db]} _]
   (let [fch (get-in db (conj form-path :sf-config :chambers 0))
         ch (get-in db (conj data-path :sf-config :chambers 0))
         sn (if (chk? (:section-count fch)) (:section-count ch))
         pdn (if (chk? (:peep-door-count fch)) (:peep-door-count ch))
         tn (if (chk? (:tube-count fch)) (:tube-count ch))
         pdn (if (and sn pdn (zero? (mod pdn sn))) (quot pdn sn))
         tn (if (and sn tn (zero? (mod tn sn))) (quot tn sn))
         pdtcs (if (and pdn tn) (vec (flatten (repeat sn (distribute tn pdn)))))]
     (if pdtcs
       {:db (-> db
                (update-in (conj data-path :sf-config :chambers)
                           (fn [chs] (mapv #(assoc % :peep-door-tube-count pdtcs) chs)))
                (assoc-in (conj form-path :sf-config :chambers 0 :peep-door-tube-count)
                          (vec (repeat pdn nil))))}))))

(rf/reg-event-fx
 ::validate-sf-chamber
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} _]
   (let [{:keys [tube-count section-count]
          pd-count :peep-door-count
          pd-tube-counts :peep-door-tube-count
          burner-count :burner-count-per-row}
         (get-in data [:sf-config :chambers 0])
         err (cond
               ;; tube count / section count
               (and section-count tube-count (pos? (mod tube-count section-count)))
               "tube count should be whole multiple of section count!"
               ;; burner count / section count
               (and section-count burner-count (pos? (mod burner-count section-count)))
               "burner count per row should be whole multiple of section count!"
               ;; peep door count / section count
               (and section-count pd-count (pos? (mod pd-count section-count)))
               "peep door count should be whole multiple of section count!"
               ;; check sum of tube count in all peep doors
               (and tube-count
                    (if-let [tns (not-empty (remove nil? pd-tube-counts))]
                      (not= (apply + tns) tube-count)))
               "sum of tube counts in all peep doors should be same as tube count!")]
     {:db (assoc-in db (conj form-path :sf-config :chamber-validity)
                    (if err {:error err, :valid? false}))})))

(defn set-sf-numbers-selection
  [db data sel-type ch-index sel-id numbers-options]
  (let [[start end] (some #(if (= (:id %) sel-id) (:nums %))
                          numbers-options)
        [start-key end-key sel-key]
        (case sel-type
          :tube [:start-tube :end-tube :tube-numbers-selection]
          :burner [:start-burner :end-burner :burner-numbers-selection])]
    (-> db
        (assoc-in data-path
                  (update-in data [:sf-config :chambers ch-index]
                             assoc start-key start, end-key end))
        (assoc-in (conj form-path :sf-config :chambers ch-index sel-key) sel-id))))

(rf/reg-event-fx
 ::set-sf-tube-numbers-selection
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/sf-tube-numbers-options])]
 (fn [{:keys [db ::subs/data ::subs/sf-tube-numbers-options]}
     [_ ch-index sel-id]]
   {:db (set-sf-numbers-selection db data :tube ch-index sel-id
                                  sf-tube-numbers-options)}))

(rf/reg-event-fx
 ::set-sf-burner-numbers-selection
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/sf-burner-numbers-options])]
 (fn [{:keys [db ::subs/data ::subs/sf-burner-numbers-options]}
     [_ ch-index sel-id]]
   {:db (set-sf-numbers-selection db data :burner ch-index sel-id
                                  sf-burner-numbers-options)}))
