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
            [tta.dialog.dataset-settings.subs :as subs]
            [tta.component.dataset.subs :as dataset-subs]
            [tta.app.subs :as app-subs]))

;; Do NOT use rf/subscribe
;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]

(defonce ^:const dlg-path [:dialog :dataset-settings])
(defonce ^:const draft-path (conj dlg-path :draft))
(defonce ^:const data-path (conj dlg-path :data))
(defonce ^:const form-path (conj dlg-path :form))

(defn parse-logger-data [logger-data]
  ;;TODO:
  {})

(defn parse [{:keys [gold-cup? dataset logger-data]}
             plant topsoe? user-roles client-id]
  
  (let [plant-settings (:settings plant)
        draft (cond-> (or dataset
                          (if logger-data (-> (parse-logger-data logger-data)
                                              (assoc :draft? true)))
                          {:draft? true})
                gold-cup? (assoc :gold-cup? true))

        pyrometer (or (:pyrometer draft)
                      (first (filter #(= (:pyrometer-id plant-settings)
                                         (:id %))
                                     (:pyrometers plant-settings))))
        settings {:plant-id (:id plant)
                  :client-id client-id
                  :summary nil
                  :data-date (htu/to-date-time-map (or (:data-date draft)
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
                                 (first user-roles))
                  :reformer-version (or (:reformer-version dataset)
                                        (get-in plant [:config :version]))}
        
        form {:emissivity-setting (if-not (:emissivity-setting settings)
                                    (missing-field))}]
    {:draft draft, :settings settings, :form form}))

(rf/reg-event-fx
 ::open
 [(inject-cofx ::inject/sub [::app-subs/plant])
  (inject-cofx ::inject/sub [::ht-subs/topsoe?])
  (inject-cofx ::inject/sub [::ht-subs/user-roles])
  (inject-cofx ::inject/sub [::app-subs/client])]
 (fn [{:keys [db ::app-subs/plant ::ht-subs/topsoe?
             ::ht-subs/user-roles ::app-subs/client]} [_ params]]
   (let [{:keys [draft settings form]} (parse params plant topsoe?
                                              user-roles (:id client))]
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
   (let [tube-emissivity (:tube-emissivity active-pyrometer)]
     ;; (js/console.log emissivity-type)
     {:db (if (= "common" emissivity-type)
            ;; in case of common ensure tube-emissivity
            (cond-> db
              (not (or tube-emissivity emissivity))
              (assoc-in (conj form-path :emissivity) (missing-field)))
            ;; otherwise emissivity override not applicable
            (update-in db form-path dissoc :emissivity))})))

(rf/reg-event-fx
 ::close
 (fn [{:keys [db]} [_ success?]]
   (cond->
       {:db (assoc-in db [:dialog :dataset-settings] nil)}
     (and (not success?) (get-in db (conj draft-path :draft?)))
     (assoc :dispatch [:tta.component.root.event/activate-content :home]))))

(defn init-sf-dataset [draft plant])


;; initialize top-fired with 5 empty wall entries 
(defn init-tf-dataset [draft plant]
  (let [tf-config (get-in plant [:config :tf-config])
        wall-temp (map (fn [] {:avg nil
                              :temps [nil nil]})
                       (repeat 5 []))

        [level-count row-count tube-counts wall-names]
        [(->> (:measure-levels tf-config)
              (filter val)
              (count))
         (:tube-row-count tf-config)
         (map :tube-count (:tube-rows tf-config))
         (map key (:wall-names tf-config))]]
    (assoc draft :top-fired
           {:levels
            (mapv (fn [level lvl-ind]
                    {:rows
                     (mapv (fn [tube-count row row-ind]
                             {:sides
                              (mapv (fn [side]
                                      {:tubes
                                       (mapv (fn [tube tube-ind]
                                               {:raw-temp nil})
                                             (repeat tube-count {})
                                             (range 0 tube-count ))})
                                    (repeat 2 {})
                                    )})
                           tube-counts
                           (repeat row-count {})
                           (range 0 row-count))} )
                  (repeat level-count {})
                  (range 0 level-count))
            :wall-temps {:north wall-temp
                         :east wall-temp
                         :west wall-temp
                         :south wall-temp}})))

(rf/reg-event-fx
 ::submit
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/can-submit?])
  (inject-cofx ::inject/sub [::app-subs/plant])
  (inject-cofx ::inject/sub [::subs/firing])]
 (fn [{:keys [db ::subs/data ::app-subs/plant ::subs/can-submit? ::subs/firing]} _]
   (merge
    (when can-submit?
      (let [draft (-> (get-in db draft-path)
                      (merge
                       (select-keys data [:plant-id :client-id :summary 
                                          :data-date :topsoe? :gold-cup?
                                          :reformer-version
                                          :pyrometer :shift :comment :operator
                                          :role-type :reformer-version]))
                      (update :data-date htu/from-date-time-map)
                      (assoc-in [:pyrometer :emissivity-setting]
                                (:emissivity-setting data)))
            draft (cond-> draft
                    (:draft? draft) (assoc :last-saved (js/Date.)))

            draft (if (or (:top-fired draft) (:sf-config draft))
                    draft
                    (case firing
                      "side" (init-sf-dataset draft plant)
                      "top" (init-tf-dataset draft plant)))]
        (cond->
            {:dispatch-n (list
                          [::close true]
                          [:tta.component.dataset.event/init {:dataset draft}])}
          ;; in case of draft, also save to local storage
          (:draft? draft)
          (assoc :storage/set {:key :draft :value draft}))))
    {:db (update-in db dlg-path assoc :show-error? true)})))

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
   {:db (set-field-decimal db [:emissivity-setting]
                           value data data-path form-path true
                           {:min 0.01 :max 1})}))

(rf/reg-event-fx
 ::set-emissivity-type
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ value]]
   {:dispatch [::validate-emissivity-type]
    :db (set-field db [:emissivity-type] value data data-path form-path true)}))

(rf/reg-event-fx
 ::set-pyrometer
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ pyrometer]]
   {:dispatch [::validate-emissivity-type]
    :db (set-field db [:pyrometer] pyrometer data data-path form-path true)}))
