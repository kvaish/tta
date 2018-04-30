;; events for dialog view-factor
(ns tta.dialog.view-factor.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [tta.util.common :refer [set-field-decimal]]
            [tta.app.event :as app-event]
            [tta.dialog.view-factor.subs :as subs]))

;; Do NOT use rf/subscribe
;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]

(defonce dlg-path [:dialog :view-factor])
(defonce data-path (conj dlg-path :data))
(defonce form-path (conj dlg-path :form))
(defonce view-path (conj dlg-path :view))

#_(defn config-not-matching? [level-opts config]
  ;; if vf not available yet, then there is no mismatch
  ;; need to check for mismatch only when present
  (if-let [vf (get-in config [:tf-config :view-factor])]
    (or
     ;; bad if list of levels not same
     (not= (into #{} (map :id level-opts))
           (into #{} (keys vf)))
     ;; now check if any level doesn't match in terms off
     ;; tube-row-count or tube-count
     (let [{:keys [tube-rows tube-row-count]} (:tf-config config)]
       (some (fn [level-key]
               (let [rows (get-in vf [level-key :tube-rows])]
                 (or
                  ;; bad if tube row count has changed
                  (not= (count rows) tube-row-count)
                  ;; just check length of one side of the wall
                  ;; its enough since this component always creates
                  ;; a complete row together with same tube-count
                  ;; for wall as well as ceiling/floor
                  (some (fn [[i wall]]
                          (let [{:keys [tube-count]} (get tube-rows i)]
                            (not= (count (first wall)) tube-count)))
                        (map-indexed (fn [i row] [i (:wall row)])
                                     rows)))))
             (map :id level-opts))))))

(rf/reg-event-fx
 ::open
 [(inject-cofx ::inject/sub [::subs/level-opts])
  (inject-cofx ::inject/sub [::subs/config])]
 (fn [{:keys [db ::subs/level-opts ::subs/config]} [_ reset? options]]
   (let [init (subs/init-data level-opts config)]
     ;; if config changes no longer matching existing view-factors
     ;; reset to nils and start again.
     {:db (update-in db dlg-path merge options {:open? true
                                                :data (if reset? init)
                                                :form init})})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db dlg-path merge  {:form nil, :data nil, :view nil}
              options {:open? false})))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :view-factor] merge options)))

(rf/reg-event-fx
 ::submit
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [::subs/data]} _]
   {:dispatch-n (list
                 [:tta.component.config.event/set-view-factor data]
                 [::close])}))

(rf/reg-event-db
 ::set-fill-all
 (fn [db [_ wall-type value]]
   (set-field-decimal db [:fill-all wall-type] value nil
                      nil form-path false
                      {:max 0.99, :min 0.01, :precision 2})))

(rf/reg-event-db
 ::set-row-selection
 (fn [db [_ value]]
   (assoc-in db (conj view-path :row-selection) value)))

(rf/reg-event-db
 ::set-selected-level
 (fn [db [_ value]]
   (assoc-in db (conj view-path :selected-level) value)))

(rf/reg-event-fx
 ::set-view-factor-field
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_  level-key row wall-type side index value]]
   {:db (set-field-decimal db [level-key :tube-rows row wall-type side index]
                           value data
                           data-path form-path false
                           {:max 0.99, :min 0.01, :precision 2})}))

(rf/reg-event-fx
 ::clear-tube-row
 [(inject-cofx ::inject/sub [::subs/data])]
 (fn [{:keys [db ::subs/data]} [_ level-key row-index tube-count]]
   (let [init-value (subs/init-tube-row level-key tube-count)]
     {:db (-> db
              (assoc-in data-path (assoc-in data [level-key :tube-rows row-index]
                                            init-value))
              (assoc-in (conj form-path level-key :tube-rows row-index)
                        init-value))})))

(rf/reg-event-fx
 ::fill-all
 [(inject-cofx ::inject/sub [::subs/data])
  (inject-cofx ::inject/sub [::subs/config])
  (inject-cofx ::inject/sub [::subs/row-selection])]
 (fn [{:keys [db ::subs/data ::subs/config ::subs/row-selection]}
     [_ level-key wall-type value]]
   (let [{:keys [tube-rows tube-row-count]} (:tf-config config)
         value (js/Number value)]
     {:db
      (reduce (fn [db i]
                (let [{:keys [tube-count]} (get tube-rows i)
                      fill (->> (repeat tube-count value) vec (repeat 2) vec)
                      init (->> (repeat tube-count nil) vec (repeat 2) vec)]
                  (-> db
                      (assoc-in (conj data-path level-key :tube-rows i wall-type)
                                fill)
                      (assoc-in (conj form-path level-key :tube-rows i wall-type)
                                init))))
              (assoc-in db data-path data)
              (if (= :all row-selection)
                (range tube-row-count)
                [row-selection]))})))
