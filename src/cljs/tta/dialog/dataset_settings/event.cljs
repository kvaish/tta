;; events for dialog dataset-settings
(ns tta.dialog.dataset-settings.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [cljs-time.core :as t]
            [cljs-time.coerce :as tc]
            [ht.app.event :as ht-event]
            [ht.app.subs :as ht-subs]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-decimal
                                            validate-field parse-value]]
            [ht.util.common :as htu]
            [tta.app.event :as app-event]
            [tta.dialog.dataset-settings.subs :as subs]))

;; Do NOT use rf/subscribe
;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]

#_(defonce ^:const dlg-path [:dialog :dataset-settings])
#_(defonce ^:const draft-path (conj dlg-path :draft))
#_(defonce ^:const data-path (conj dlg-path :data))
#_(defonce ^:const form-path (conj dlg-path :form))

(def  dlg-path [:dialog :dataset-settings])
(def  draft-path (conj dlg-path :draft))
(def  data-path (conj dlg-path :data))
(def  form-path (conj dlg-path :form))

(defn parse-logger-data [logger-data]
  ;;TODO:
  {})

(defn parse [{:keys [gold-cup? dataset logger-data]}
             plant-settings topsoe? user-roles]
  (let [draft (cond-> (or dataset
                          (if logger-data (-> (parse-logger-data logger-data)
                                              (assoc :draft? true)))
                          {:draft? true})
                gold-cup? (assoc :gold-cup? true))

        pyrometer (or (:pyromerter draft)
                      (first (filter #(= (:pyrometer-id plant-settings)
                                         (:id %))
                                     (:pyrometers plant-settings))))
        settings {:data-date (htu/to-date-time-map (or (:data-date draft)
                                                       (js/Date.)))
                  :topsoe?    (if dataset (:topsoe? draft)
                                  topsoe?)

                  :pyrometer pyrometer
                  :emissivity-type (or (:emissivity-type draft)
                                       (:emissivity-type plant-settings))
                  :emissivity (:emissivity draft)
                  :emissivity-setting (:emissivity-setting pyrometer) 
                  :shift  (:shift draft)
                  :comment (:comment draft)
                  :operator (:operator draft)
                  :role-type (or (:role-type draft)
                                 (first user-roles))}
        form {:emissivity-setting (if-not (:emissivity-setting settings)
                                    (missing-field))}]
    {:draft draft, :settings settings, :form form}))

(rf/reg-event-fx
 ::open
 [(inject-cofx ::inject/sub [::subs/settings])
  (inject-cofx ::inject/sub [::ht-subs/topsoe?])
  (inject-cofx ::inject/sub [::ht-subs/user-roles])]
 (fn [{:keys [db ::subs/settings ::ht-subs/topsoe? ::ht-subs/user-roles]} [_ params]]
   (let [{:keys [draft settings form]} (parse params settings topsoe? user-roles)]
     {:dispatch [::validate-emissivity-type]
      :db (assoc-in db [:dialog :dataset-settings] {:open? true
                                                    :draft draft
                                                    :settings settings
                                                    :form form})})))

(rf/reg-event-fx
 ::validate-emissivity-type
 [(inject-cofx ::inject/sub [::subs/active-pyrometer])
  (inject-cofx ::inject/sub [::subs/emissivity-type])
  (inject-cofx ::inject/sub [::subs/emissivity])]
 (fn [{:keys [db ::subs/active-pyrometer
             ::subs/emissivity-type
             ::subs/emissivity]} _]
   (let [tube-emissivity (:tube-emissivity active-pyrometer)
         emissivity-field (if (or tube-emissivity emissivity) (make-field emissivity) (missing-field))]
     (js/console.log emissivity-type)
     {:db (cond (= "common" emissivity-type)
                (assoc-in db (conj form-path :emissivity) emissivity-field)
                (not (= "common" emissivity-type))
                (-> db
                    (update-in form-path #(dissoc % :emissivity))
                    (update-in data-path #(dissoc % :emissivity)))
                "default" db
                )})))

(rf/reg-event-fx
 ::close
 (fn [{:keys [db]} _]
   (cond->
       {:db (assoc-in db [:dialog :dataset-settings] nil)}
     (get-in db (conj draft-path :draft?))
     (assoc :dispatch [:tta.component.root.event/activate-content :home]))))

(rf/reg-event-fx
 ::submit
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} _]
   (let [draft (merge data (get-in db draft-path))]
     {:dispatch-n (list
                   [::close]
                   [:tta.component.dataset.event/init {:dataset draft}])
      :storage/set {:key :draft :value draft}})))


(rf/reg-event-fx
 ::set-field
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path value required?]]
   {:db (set-field db path value data data-path form-path required?)}))


(rf/reg-event-fx
 ::set-override-emissivity
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/active-pyrometer])]
 (fn [{:keys [db ::subs/data ::subs/active-pyrometer]} [_ value]]
   (let [required? (if-not (:tube-emissivity active-pyrometer) true false)]
     {:db (set-field-decimal db [:emissivity] value data data-path form-path
                             required?
                             {:min 0.01 :max 0.99})})))


(rf/reg-event-fx
 ::set-emissivity-setting
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ value]]
   {:db (set-field-decimal db [:emissivity-setting] value data data-path form-path true
                           {:min 0.01 :max 1})}))

(rf/reg-event-fx
 ::set-emissivity-type
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ value]]
   {:dispatch [::validate-emissivity-type]
    :db (set-field db [:emissivity-type] value data data-path form-path true)}))


(rf/reg-event-fx
 ::set-field
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path value required?]]
   {:db (set-field db path value data data-path form-path required?)}))

(rf/reg-event-fx
 ::set-pyrometer
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/pyrometers])]
 (fn [{:keys [db ::subs/data ::subs/pyrometers]} [_ id]]
   {:dispatch [::validate-emissivity-type]
    :db (set-field db [:pyrometer]
                   (some #(if (= id (:id %)) %) pyrometers)
                   data data-path form-path true)}))
