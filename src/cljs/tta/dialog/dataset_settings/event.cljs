;; events for dialog dataset-settings
(ns tta.dialog.dataset-settings.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [cljs-time.core :as t]
            [cljs-time.coerce :as tc]
            [ht.util.common :as u]
            [ht.app.event :as ht-event]
            [ht.app.subs :as ht-subs]
            [tta.util.common :as au :refer [make-field missing-field
                                            set-field set-field-text
                                            set-field-decimal
                                            validate-field parse-value]]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.dialog.dataset-settings.subs :as subs]))

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
                  :data-date (if-let [date (:data-date draft)]
                               (u/to-date-time-map date)
                               (-> (u/to-date-time-map (js/Date.))
                                   (update :minute #(* 5 (quot % 5)))
                                   (assoc :second 0)))
                  :topsoe? (if dataset (:topsoe? draft) topsoe?)
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
        form {:pyrometer {:emissivity-setting (if-not (:emissivity-setting settings)
                                                (missing-field))}}]
    ;; (js/console.log settings)
    {:draft draft, :settings settings, :form form}))

(rf/reg-event-fx
 ::open
 [(inject-cofx ::inject/sub [::app-subs/plant])
  (inject-cofx ::inject/sub [::ht-subs/topsoe?])
  (inject-cofx ::inject/sub [::ht-subs/user-roles])
  (inject-cofx ::inject/sub [::app-subs/client])]
 (fn [{:keys [db ::app-subs/plant ::ht-subs/topsoe?
             ::ht-subs/user-roles ::app-subs/client]} [_ params]]
   ;; (js/console.log "dialog open")
   (let [{:keys [draft settings form]} (parse params plant topsoe?
                                              user-roles (:id client))]
     {:dispatch [::validate-emissivity-type]
      :db (assoc-in db [:dialog :dataset-settings]
                    (assoc {:open? true
                            ;; whether starting totally new
                            :new? (not (:dataset params))
                            :draft draft
                            :form form}
                           (if (:dataset params) :src-data :data)
                           settings))})))

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
     (and (not success?) (get-in db (conj dlg-path :new?)))
     (assoc :dispatch [:tta.component.root.event/activate-content :home]))))



(defn init-sf-dataset [draft plant]
  (let [sf-config (get-in plant [:config :sf-config])
        ch-count (count (:chambers sf-config))
        pd-count (get-in sf-config [:chambers 0 :peep-door-count])
        tube-count (get-in sf-config [:chambers 0 :tube-count])]
    (assoc draft :side-fired
           {:chambers
            (->> {:sides (->> {:tubes (vec (repeat tube-count nil))
                               :wall-temps (->> {:temps (vec (repeat 5 nil))}
                                                (repeat pd-count)
                                                (vec))}
                              (repeat 2)
                              (vec))}
                 (repeat ch-count)
                 (vec))})))

(defn init-tf-dataset [draft plant]
  (let [tf-config (get-in plant [:config :tf-config])
        temps {:temps (vec (repeat 5 nil))}
        {:keys [top? middle? bottom?]} (:measure-levels tf-config)
        row-count (get-in plant [:config :tf-config :tube-row-count])
        tube-counts (map :tube-count (:tube-rows tf-config))
        level {:rows
               (mapv (fn [tube-count]
                       {:sides (->> {:tubes (vec (repeat tube-count nil))}
                                    (repeat 2)
                                    (vec))})
                     tube-counts)}]
    (assoc draft :top-fired
           (cond->
               {:levels (cond-> {}
                          top? (assoc :top level)
                          middle? (assoc :middle level)
                          bottom? (assoc :bottom level))
                :wall-temps {:north temps
                             :east temps
                             :west temps
                             :south temps}}
             top? (assoc :ceiling-temps (vec (repeat (inc row-count) temps)))
             bottom? (assoc :floor-temps (vec (repeat (inc row-count) temps)))))))

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
                       (select-keys data [:plant-id :client-id :pyrometer
                                          :data-date :reformer-version
                                          :topsoe? :gold-cup? :summary
                                          :emissivity :emissivity-type
                                          :shift :comment :operator :role-type]))
                      (update :data-date u/from-date-time-map))
            draft (cond-> draft
                    ;; draft dataset also saved to storage
                    ;; hence update the last saved time to current time
                    (:draft? draft) (assoc :last-saved (js/Date.))
                    ;; when not initialized add either :top-fired or :side-fired
                    (and (= firing "side") (not (:side-fired draft)))
                    (init-sf-dataset plant)
                    (and (= firing "top") (not (:top-fired draft)))
                    (init-tf-dataset plant))]
        (cond->
            {:dispatch-n (list
                          [::close true]
                          [:tta.component.dataset.event/init {:dataset draft}])}
          ;; in case of draft, also save to local storage
          (:draft? draft)
          (assoc :storage/set {:key :draft
                               :value (au/dataset-to-storage draft)}))))
    {:db (update-in db dlg-path assoc :show-error? true)})))

(rf/reg-event-fx
 ::set-field
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ path value required?]]
   {:db (set-field db path value data data-path form-path required?)}))

(rf/reg-event-fx
 ::set-emissivity
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/active-pyrometer])]
 (fn [{:keys [db ::subs/data ::subs/active-pyrometer]} [_ value]]
   (let [required? (if-not (:tube-emissivity active-pyrometer) true false)]
     {:db (set-field-decimal db [:emissivity] value data data-path form-path
                             required?
                             {:min 0.01 :max 0.99})})))

(rf/reg-event-fx
 ::set-emissivity-type
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/src-data])]
 (fn [{:keys [db ::subs/data ::subs/src-data]} [_ value]]
   {:dispatch [::validate-emissivity-type]
    :db (-> db
            (set-field [:emissivity-type] value data
                       data-path form-path true)
            (update-in data-path assoc :emissivity
                       ;; restore the emissivity override, if common is chosen
                       ;; otherwise clear it
                       (if (= value "common") (:emissivity src-data))))}))

(rf/reg-event-fx
 ::set-pyrometer
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ pyrometer]]
   {:dispatch [::validate-emissivity-type]
    :db (-> db
            (assoc-in data-path (assoc data :pyrometer pyrometer))
            (assoc-in (conj form-path :pyrometer :emissivity-setting)
                      (missing-field)))}))

(rf/reg-event-fx
 ::set-pyrometer-emissivity-setting
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ value]]
   {:db (set-field-decimal db [:pyrometer :emissivity-setting]
                           value data data-path form-path true
                           {:min 0.01 :max 1})}))
