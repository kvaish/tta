(ns tta.component.dataset.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as tc]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.common :as au]
            [tta.util.auth :as auth]))

;; primary signals
(rf/reg-sub
 ::component
 (fn [db _]
   (get-in db [:component :dataset])))

(rf/reg-sub
 ::show-error? ;; used for hiding errors until first click on submit
 :<- [::component]
 (fn [component _] (:show-error? component)))

;; derived signals/subscriptions
(rf/reg-sub
 ::src-data
 :<- [::component]
 (fn [component _] (:dataset component)))

(rf/reg-sub
 ::data
 :<- [::src-data]
 :<- [::component]
 (fn [[src-data component] _] (or (:data component) src-data)))

(rf/reg-sub
 ::form
 :<- [::component]
 (fn [component _] (:form component)))

(rf/reg-sub
 ::view
 :<- [::component]
 (fn [component _] (:view component)))

(rf/reg-sub
 ::dirty?
 :<- [::data]
 :<- [::src-data]
 (fn [[data src-data] _] (not= data src-data)))

(rf/reg-sub
 ::valid?
 :<- [::form]
 (fn [form _] (not (au/some-invalid form))))


(rf/reg-sub
 ::can-submit?
 :<- [::dirty?]
 :<- [::valid?]
 (fn [[dirty? valid?] _] (and dirty? valid?)))

(rf/reg-sub
 ::can-upload?
 :<- [::dirty?]
 :<- [::valid?]
 :<- [::data]
 (fn [[dirty? valid? data] _] (and (or dirty? (:draft? data)) valid?)))

(rf/reg-sub
 ::warn-on-close?
 :<- [::dirty?]
 :<- [::valid?]
 (fn [[dirty? valid?] _] (or dirty? (not valid?))))

(rf/reg-sub
 ::can-edit?
 :<- [::reformer-version]
 :<- [::app-subs/plant]
 :<- [::ht-subs/auth-claims]
 (fn [[ref-version plant claims] _]
   (and (auth/allow-edit-dataset? claims)
        (= ref-version (get-in plant [:config :version])))))

(rf/reg-sub
 ::can-delete?
 :<- [::ht-subs/auth-claims]
 (fn [claims _]
   (auth/allow-delete-dataset? claims)))

(rf/reg-sub
 ::can-export?
 :<- [::ht-subs/auth-claims]
 (fn [claims _]
   (auth/allow-export? claims)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-field
  ([path form data] (get-field path form data identity))
  ([path form data parse]
   (or (get-in form path)
       {:value (parse (get-in data path))
        :valid? true})))

(rf/reg-sub
 ::field
 :<- [::form]
 :<- [::data]
 (fn [[form data] [_ path]] (get-field path form data)))

;; PLANT ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::config
 :<- [::reformer-version]
 :<- [::app-subs/plant]
 (fn [[version plant] _]
   (if (= version (get-in plant [:config :version]))
     (:config plant)
     (some #(if (= version (:version %)) %) (:history plant)))))

(rf/reg-sub
 ::settings
 :<- [::data-date]
 :<- [::firing]
 :<- [::app-subs/plant]
 (fn [[data-date firing plant] _]
   ;; ensures correct tube-prefs(pinch) and design/target temp
   ;; as on the date the reading was taken (data-date)
   ;;
   ;; returns nil when data not ready

   (when-let [data-date (tc/from-date data-date)]
     (let [{:keys [settings]} plant
           pinch-old (some #(if (t/before? data-date
                                           (tc/from-date (:end-date %)))
                              %)
                           (reverse (:pinch-history settings)))
           std-temp-old (some #(if (t/before? data-date
                                              (tc/from-date (:end-date %)))
                                 %)
                              (reverse (:std-temp-history settings)))
           settings (if std-temp-old
                      ;; update the current with old values
                      (let [{:keys [target design]} std-temp-old]
                        (assoc settings
                               :target-temp target
                               :design-temp design))
                      ;; use current
                      settings)]
       (if pinch-old
         ;; updated the current with old values
         (update-in settings
                    (case firing
                      "side" [:sf-settings :chambers]
                      [:tf-settings :tube-rows])
                    (fn [rows]
                      (first
                       (reduce (fn [[rows i] prefs]
                                 [(update-in rows [i :tube-prefs]
                                             (fn [row]
                                               (let [n (count row)]
                                                 (reduce #(assoc %1 %2 "pin")
                                                         row
                                                         (filter #(< % n) prefs)))))
                                  (inc i)])
                               [rows 0]
                               (take (count rows) (:tubes pinch-old))))))
         ;; use current
         settings)))))

(rf/reg-sub
 ::firing
 :<- [::config]
 (fn [config _] (:firing config)))

;; VIEW STATE ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub-raw
 ::mode-opts
 (fn [_ _]
   (reaction
    (let [mode-read (translate [:dataset :mode :read] "Graph")
          mode-edit (translate [:dataset :mode :edit] "Data")]
      [{:id :view
        :label mode-read}
       {:id :edit
        :label mode-edit}]))))

(rf/reg-sub
 ::mode
 :<- [::view]
 (fn [view _] (:mode view)))

(rf/reg-sub-raw
 ::area-opts ;; the top tab selection options
 (fn [_ _]
   (reaction
    (let [firing @(rf/subscribe [::firing])
          gold-cup? @(rf/subscribe [::gold-cup?])
          mode @(rf/subscribe [::mode])
          burner? @(rf/subscribe [::burner?])]
      (->> [{:id :overall
             :label (translate [:data-analyzer :area :overall] "Overall")
             :show? (and (= mode :view) (= firing "top"))}
            {:id :twt
             :label (case mode
                      :view (translate [:data-entry :area :twt] "TWT")
                      (translate [:data-analyzer :area :twt] "Tube/Wall"))
             :show? true}
            {:id :burner-status
             :label (translate [:data-entry :area :burner] "Burner Status")
             :show? (or (= mode :read))}
            {:id :burner
             :label (translate [:data-entry :area :burner] "Burners")
             :show? (or (= mode :edit) burner?)}
            {:id :gold-cup
             :label (translate [:data-entry :area :gold-cup] "Gold Cup")
             :show? gold-cup?}]
           (filter :show?))))))

(rf/reg-sub
 ::selected-area ;; the top tab selected
 :<- [::view]
 (fn [view _] (or (:selected-area view) 0)))

(rf/reg-sub-raw
 ::level-opts ;; bottom tab selection options
 (fn [_ _]
   (reaction
    (let [config @(rf/subscribe [::config])]
      (case (:firing config)
        "side" [{:id :reformer
                 :label (translate [:data-entry :levels :reformer] "Reformer")}]
        (->> [{:id :top
               :label (translate [:data-entry :levels :top] "Top")
               :show? (get-in config [:tf-config :measure-levels :top?])}
              {:id :middle
               :label (translate [:data-entry :levels :middle] "Middle")
               :show? (get-in config [:tf-config :measure-levels :middle?])}
              {:id :bottom
               :label (translate [:data-entry :levels :bottom] "Bottom")
               :show? (get-in config [:tf-config :measure-levels :bottom?])}]
             (filter :show?)
             (vec)))))))

(rf/reg-sub
 ::selected-level ;; bottom tab selected
 :<- [::view]
 (fn [view _] (or (:selected-level view) 0)))

(rf/reg-sub
 ::selected-level-key
 :<- [::selected-level]
 :<- [::level-opts]
 (fn [[index opts] _] (get-in opts [index :id])))

(rf/reg-sub
 ::level-key
 :<- [::level-opts]
 (fn [opts [_ index]] (get-in opts [index :id])))

(rf/reg-sub
 ::twt-entry-mode ;; :partial or :full
 :<- [::view]
 (fn [view _] (or (:twt-entry-mode view) :partial)))

(rf/reg-sub-raw
 ::twt-entry-scope-opts
 (fn [_ [_ level-key]]
   (reaction
    (->> (let [level-key (or level-key @(rf/subscribe [::selected-level-key]))]
           [{:id :tube
             :label (translate [:data-entry :twt-scope :tube] "Tube")
             :show? true}
            {:id :wall
             :label (translate [:data-entry :twt-scope :wall] "Wall")
             :show? true}
            {:id :ceiling
             :label (translate [:data-entry :twt-scope :ceiling] "Ceiling")
             :show? (= level-key :top)}
            {:id :floor
             :label (translate [:data-entry :twt-scope :floor] "Floor")
             :show? (= level-key :bottom)}])
         (filter :show?)))))

(rf/reg-sub
 ::twt-entry-scope
 :<- [::view]
 :<- [::level-key]
 (fn [[view current-level-key] [_ level-key]]
   (get-in view [:twt-entry-scope (or level-key current-level-key)] :tube)))

(rf/reg-sub
 ::twt-entry-index
 :<- [::view]
 (fn [view [_ scope]] (get-in view [:twt-entry-index scope]
                             (if (= scope :wall) :north 0))))

(rf/reg-sub
 ::twt-entry-nav-disabled?
 (fn [[_ scope _] _]
   [(rf/subscribe [::config])
    (rf/subscribe [::twt-entry-index scope])])
 (fn [[config index] [_ scope dir]]
   (if (= scope :wall) false
       (case dir
         :next (let [tc (get-in config [:tf-config :tube-row-count])
                     n (if (= scope :tube) (dec tc) tc)]
                 (= n index))

         :prev (= 0 index)
         nil))))

;; DATASET ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::dataset-id
 :<- [::data]
 (fn [data _] (:id data)))

(rf/reg-sub
 ::data-date
 :<- [::data]
 (fn [data _] (:data-date data)))

(rf/reg-sub
 ::gold-cup?
 :<- [::data]
 (fn [data _] (:gold-cup? data)))

(rf/reg-sub
 ::burner? ;; whether the dataset has burner data
 :<- [::data]
 :<- [::firing]
 (fn [[data firing] _]
   (case firing
     "top" (some? (get-in data [:top-fired :burners]))
     "side" (some? (get-in data [:side-fired :chambers 0 :sides 0 :burners]))
     nil)))

(rf/reg-sub
 ::reformer-version
 :<- [::data]
 (fn [data _] (:reformer-version data)))

(rf/reg-sub
 ::last-saved
 :<- [::data]
 (fn [data _]
   ((some-fn :last-saved :date-modified :date-created) data)))

(rf/reg-sub
 ::sf-burner
 :<- [::data]
 (fn [data [_ [ch-index side row col]]]
   (get-in data [:side-fired :chambers ch-index :sides side
                 :burners row col :state])))

(rf/reg-sub
 ::tf-burner
 :<- [::data]
 (fn [data [_ [row col]]]
   (get-in data [:top-fired :burners row col :deg-open])))

(defn get-field-temp [path form data temp-unit]
  (get-field path form data #(au/to-temp-unit % temp-unit)))

(rf/reg-sub
 ::field-temp
 :<- [::form]
 :<- [::data]
 :<- [::app-subs/temp-unit]
 (fn [[form data temp-unit] [_ path]]
   (get-field-temp path form data temp-unit)))

(rf/reg-sub
 ::has-raw-temp
 :<- [::data]
 :<- [::form]
 (fn [[data form] [_ row-path]]
   (->> [data form]
        (some (fn [d]
                (->> (get-in d (conj row-path :sides))
                     (some (fn [side]
                             (->> (:tubes side)
                                  (some :raw-temp))))))))))

;; wall temps count given the path to the array
(rf/reg-sub
 ::wall-temps-count
 :<- [::data]
 (fn [data [_ path]]
   (count (get-in data path))))

;; check if has any wall temp given the path to the array
(rf/reg-sub
 ::has-wall-temps
 :<- [::data]
 :<- [::form]
 (fn [[data form] [_ path]]
   (->> [data form]
        (some (fn [d]
                (->> (get-in d path)
                     (some some?)))))))

(rf/reg-sub
 ::tube-prefs
 :<- [::settings]
 :<- [::firing]
 (fn [[settings firing] [_ row-index]]
   (case  firing
     "top" (get-in settings [:tf-settings :tube-rows row-index :tube-prefs])
     "side" (get-in settings [:sf-settings :chambers row-index :tube-prefs])
     nil)))