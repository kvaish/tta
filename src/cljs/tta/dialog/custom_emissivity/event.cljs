;; events for dialog custom-emissivity
(ns tta.dialog.custom-emissivity.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.dialog.custom-emissivity.subs :as subs]
            [tta.component.settings.subs :as setting-subs]))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db [:dialog :custom-emissivity] merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db [:dialog :custom-emissivity] merge options {:open? false})))


(defn number [field {:keys [min max]}]

  (let [value (js/Number(:value field))]
    (if (or (< max value)
            (> min value)
            (not (number? value)))
      (assoc field :valid? false :error (str "Number should be between " min "-" max))
      field)))

(defn required [field]
  (if (empty? (:value  field))
    (assoc field :valid? false :error "This field is required.")))

(defn make-field [value]
  {:valid? true
   :error nil
   :value value})

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value validations]]
   (let [validated-field (if (:number validations)
                           (number (make-field  value) (:number validations))
                           (make-field value))]
     
     (assoc-in db [:dialog :custom-emissivity :field id] validated-field)))

     )
(rf/reg-event-db
 ::set-emissivity-field
 (fn [db [_ {:keys [side chamber tube value]} validations ]]
   (let [data-emissivity @(rf/subscribe [::subs/data-custom-emissivity])
         field (make-field value)
         validated-field (if (:number validations)
                           (number field (:number validations))
                           field)
         new-data (if (:valid? validated-field)
                    (assoc-in data-emissivity  [chamber side tube]
                              (js/Number value))
                    data-emissivity)]
     (-> db
         (assoc-in [:dialog :custom-emissivity :data :custom-emissivity] new-data)
         (assoc-in [:dialog :custom-emissivity :form chamber side tube]
                   validated-field)))))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :custom-emissivity :data] data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (into db (update-in db [:dialog :custom-emissivity] merge options))
   ))

(rf/reg-event-db
 ::reset-dialog
 (fn [db _]
   (update-in db  [:dialog :custom-emissivity] dissoc :form :data :field)))

(rf/reg-event-fx
 ::discard-data
 (fn [db]
   {:dispatch-n  (list [::reset-dialog]
                       [::close])}))

(rf/reg-event-db
 ::fill-all
 (fn [db [_ emissivity]]
   (let [plant @(rf/subscribe [::setting-subs/plant])
         firing @(rf/subscribe [::subs/firing])
         data-emissivity @(rf/subscribe [::subs/data-custom-emissivity])
         config-data  (cond
                       (= firing "side")
                       (get-in plant [:config :sf-config :chambers])
                       (= firing "top")
                       (get-in plant [:config :tf-config :rows]))
         tube-count (get-in config-data [0 :tube-count])
         side-data (vec (repeat 2 (vec (repeat tube-count 3))))
         ]
     (into db (assoc-in db [:dialog :custom-emissivity :data :custom-emissivity]
                        (vec (repeat (count config-data) side-data)))))))

(rf/reg-event-db
 ::save-data
 (fn [db]
   (let [firing @(rf/subscribe [::subs/firing])
         emissivity-data (get-in @(rf/subscribe [::subs/dialog])
                                 [:data :custom-emissivity])
         settings @(rf/subscribe [::subs/settings])
         setting-path (cond
                        (= firing "side")
                        [:component :settings :data :sf-settings :chambers]
                        (= firing "top")
                        [:component :settings :data :tf-settings :rows])]

     (into db (doall (map-indexed (fn [ind col]
                                    (print (conj setting-path  ind
                                                 :custom-emissivity))
                                    (update-in db (conj setting-path  ind
                                                        :custom-emissivity) col) 
                                    ) emissivity-data))))))
