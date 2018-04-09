;; subscriptions for component dataset
(ns tta.component.dataset.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as tc]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.common :as au]
            [tta.util.auth :as auth]))

;; Do NOT use rf/subscribe
;; instead add input signals like :<- [::query-id]
;; or use reaction or make-reaction (refer reagent docs

;; DATA STRUCTURE (primary keys)
;; :view - map representing view state
;;         :mode -> :read or :edit
;;         :selected-area -> options = :twt, :overall, :burner, :gold-cup
;;         :selected-level -> options = :top, :bottom, :middle, :reformer
;;
;; :dataset - the dataset
;;               published dataset fetched from db
;;            or draft dataset loaded from local storage
;;
;; :data - working dataset
;; :form - raw working dataset and any validation fields

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
 ::warn-on-close?
 :<- [::dirty?]
 :<- [::valid?]
 (fn [[dirty? valid?] _] (or dirty? (not valid?))))

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
             :show? (and (= mode :read) (= firing "top"))}
            {:id :twt
             :label (case mode
                      :read (translate [:data-entry :area :twt] "TWT")
                      (translate [:data-analyzer :area :twt] "Tube/Wall"))
             :show? true}
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
 (fn [view _] (:selected-area view)))

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
             (filter :show?)))))))

(rf/reg-sub
 ::selected-level ;; bottom tab selected
 :<- [::view]
 (fn [view _] (:selected-level view)))



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
 (fn [data _]))

(rf/reg-sub
 ::reformer-version
 :<- [::data]
 (fn [data _] (:reformer-version data)))

(rf/reg-sub
 ::last-saved
 :<- [::data]
 (fn [data _]
   ((some-fn :last-saved :date-modified :date-created) data)))
