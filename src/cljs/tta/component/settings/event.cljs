;; events for component setting
(ns tta.component.settings.event
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
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.app.subs :as app-subs]
            [tta.component.settings.subs :as subs]))

(defonce comp-path [:component :settings])
(defonce data-path (conj comp-path :data))
(defonce form-path (conj comp-path :form))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   (let [{:keys [pyrometer-id temp-unit emissivity-type min-tubes%]}
         @(rf/subscribe [::subs/data])]
     {:dispatch-n
      (list [::set-field [:pyrometer-id] pyrometer-id true]
            [::set-temp-unit (or temp-unit au/deg-C)]
            (if-not emissivity-type
              [::set-data-field [:emissivity-type] "common"])
            (if-not min-tubes%
              [::set-data-field [:min-tubes%] 50]))})))

(rf/reg-event-db
 ::close
 (fn [db _] (assoc-in db comp-path nil)))

(rf/reg-event-db
 ::set-data-field
 (fn [db [_ path value]]
   (let [data @(rf/subscribe [::subs/data])]
     (assoc-in db data-path (assoc-in data path value)))))

(rf/reg-event-fx
 ::sync-after-save
 [(inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::app-subs/plant]} [_ [eid]]]
   (cond-> {:forward-events {:unregister ::sync-after-save}}
     (= eid ::app-event/fetch-plant-success)
     (assoc :db (assoc-in db data-path (:settings plant))))))

(defn archive-std-temp [settings old-settings]
  (let [std-temp (select-keys old-settings [:target-temp :design-temp])]
    (if (= (select-keys settings [:target-temp :design-temp]) std-temp)
      settings ; no archiving
      (update settings :std-temp-history
              #(conj (or % []) {:target (:target-temp std-temp)
                                :design (:design-temp std-temp)})))))

(defn get-pinch-indices [settings]
  (->> (or (get-in settings [:tf-settings :tube-rows])
           (get-in settings [:sf-settings :chambers]))
       (mapv (fn [{:keys [tube-prefs]}]
               (vec (keep-indexed #(if (= "pin" %2) %1) tube-prefs))))))

(defn archive-pinch [settings old-settings]
  (let [tps (get-pinch-indices settings)
        otps (get-pinch-indices old-settings)]
    (if (= tps otps)
      settings
      (update settings :pinch-history
              #(conj (or % []) {:tubes otps})))))

(defn archive-settings [settings old-settings]
  (if (empty? old-settings)
    settings ;; no archiving on first time edit
    (-> settings
        (archive-std-temp old-settings)
        (archive-pinch old-settings))))

(rf/reg-event-fx
 ::upload
 [(inject-cofx ::inject/sub [::app-subs/client])
  (inject-cofx ::inject/sub [::app-subs/plant])]
 (fn [{:keys [db ::app-subs/client ::app-subs/plant]} _]
   (merge (when @(rf/subscribe [::subs/can-submit?])
            ;; raise save fx with busy screen and then show confirmation
            {:forward-events {:register ::sync-after-save
                              :events #{::app-event/fetch-plant-success
                                        ::ht-event/service-failure}
                              :dispatch-to [::sync-after-save]}
             :dispatch [::ht-event/set-busy? true]
             #_:service/update-plant-settings
             #_{:client-id (:id client)
                :plant-id (:id plant)
                :change-id (:change-id plant)
                :settings (archive-settings (get-in db data-path) (:settings plant))
                :evt-success [::app-event/fetch-plant (:id client) (:id plant)]}})
          {:db (update-in db comp-path assoc :show-error? true)})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field db path value data data-path form-path required?))))

(rf/reg-event-db
 ::set-text
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field-text db path value data data-path form-path required?))))

(rf/reg-event-fx
 ::set-temp-unit
 (fn [{:keys [db]} [_ %]]
   (let [data @(rf/subscribe [::subs/data])
         target-temp (subs/get-field-temp [:target-temp] nil data %)
         design-temp (subs/get-field-temp [:design-temp] nil data %)
         design-temp (if (nil? (:value design-temp))
                       (au/missing-field)
                       design-temp)]
     {:dispatch [::set-field [:temp-unit] %]
      :db (update-in db form-path
                     #(-> %
                          (assoc-in [:target-temp] target-temp)
                          (assoc-in [:design-temp] design-temp)))})))

(rf/reg-event-db
 ::set-temp
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])
         temp-unit @(rf/subscribe [::subs/temp-unit])]
     (set-field-temperature db path value data data-path form-path required? temp-unit))))

(rf/reg-event-db
 ::set-number
 (fn [db [_ path value required? {:keys [max min]}]]
   (let [data @(rf/subscribe [::subs/data])]
     (set-field-number db path value data data-path form-path required?
                       {:max max, :min min}))))

(rf/reg-event-fx
 ::set-pyrometers
 (fn [{:keys [db]} [_ pyrometers]]
   (let [data @(rf/subscribe [::subs/data])
         pid (:value @(rf/subscribe [::subs/field [:pyrometer-id]]))
         missing? (not (some #(= pid (:id %)) pyrometers))]
     {:db (assoc-in db data-path (assoc data :pyrometers pyrometers))
      :dispatch-n (list (if missing? [::set-field [:pyrometer-id] nil true]))})))

(rf/reg-event-db
 ::set-tube-prefs
 (fn [db [_ tube-prefs]]
   (let [firing (get-in @(rf/subscribe [:tta.app.subs/plant]) [:config :firing])
         old-prefs (or (get-in db (conj data-path :sf-settings :chambers))
                       (get-in db (conj data-path :tf-settings :tube-rows)))
         new-prefs (mapv (fn [col1 col2]
                           (assoc-in col1 [:tube-prefs] col2)) old-prefs tube-prefs)]
     (assoc-in db (case firing
                    "side" (conj data-path :sf-settings :chambers)
                    "top" (conj data-path :tf-settings :tube-rows))
               new-prefs))))

(rf/reg-event-db
 ::set-emissivity-type
 (fn [db [_ path value required?]]
   (let [has-custom-emissivity @(rf/subscribe [::subs/has-custom-emissivity?])
         data @(rf/subscribe [::subs/data])]
     (if (and (nil? has-custom-emissivity) (= value "custom"))
       (assoc-in db (into form-path path)
                 {:value value
                  :valid? false
                  :error
                  (translate [::settings :custom-emissivity :error]
                             "Please provide each tube emissivity")})
       (set-field db path value data data-path form-path required?)))))


;;TODO set custom emissivity
(rf/reg-event-db
 ::set-custom-emissivity
 (fn [db [_ custom-emissivity]]
   (let [data @(rf/subscribe [::subs/data])
         firing (get-in @(rf/subscribe [:tta.app.subs/plant]) [:config :firing])
         old-custom-emissivity
         (or (get-in db (conj data-path :sf-settings :chambers))
             (get-in db (conj data-path :tf-settings :levels)))
         new-custom-emissivity
         (mapv (fn [col1 col2]
                 (assoc-in col1  [:custom-emissivity] col2))
               old-custom-emissivity custom-emissivity)])))
