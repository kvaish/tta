;; events for dialog edit-pyrometer
(ns tta.dialog.edit-pyrometer.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.dialog.edit-pyrometer.subs :as subs]))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db [:dialog :edit-pyrometer] merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db [:dialog :edit-pyrometer] merge options {:open? false})))

#_(rf/reg-event-db
   ::set-field
   (fn [db [_ id value]]
     (assoc-in db [:dialog :edit-pyrometer :field id]
               {:valid? false
                :error nil
                :value value})))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :edit-pyrometer :data] data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :edit-pyrometer] merge options)))

(rf/reg-event-db
 ::set-popover-open
 (fn [db [_ open?]]
   (assoc-in db [:dialog :edit-pyrometer :popover-open?] open?)))

(defn required [field validations]
 
  (if (and (:required? validations)
           (empty? (:value  field)))
    (assoc field :valid? false :error "This field is required.")
    field))


(defn number [field validations]
  (let [value (js/Number(:value field))
        validation-keys (:number validations)
        min (:min validation-keys)
        max (:max validation-keys)
        ]
    (cond
      (and (:number validations)
           (not (number? value)) ) (assoc field :valid? false :error
                                          "Please enter number")
      (and (:number validations) 
           (or (<  max value)
               (> min value))) (assoc field :valid? false :error
                                      (str "Number should be between " min "-" max))
      :else field)))

(defn make-field [value]
  {:valid? true
   :error nil
   :value value})

(rf/reg-event-db
 ::set-field
 (fn [db [_ path value id validations]]
   (let  [data            @(rf/subscribe [::subs/data])
          pyrometers      @(rf/subscribe [::subs/pyrometers])
          field           (make-field value)
          validated-field (-> field
                              (required validations)
                              (number validations))
          
          new-data (if (:valid? validated-field )
                     (map (fn [p]
                            (if (= (:id p) id)
                              (assoc-in p path value) p)) pyrometers)
                     pyrometers)]
     (print new-data);
     (-> db
         (assoc-in (into [:dialog :edit-pyrometer :form id] path)
                   validated-field)
         (assoc-in  [:dialog :edit-pyrometer :data] new-data)))))

(rf/reg-event-db
 ::discard-field
 (fn [db [_ id]]
   (assoc-in db [:dialog :edit-pyrometer :form]
             (dissoc
              (get-in db [:dialog :edit-pyrometer :form]) id)) ))

(rf/reg-event-fx
 ::discard-popover-data
 (fn [db [_ id]]
   {:dispatch-n  (list [::discard-field id]
                       [::set-popover-open false])}))

(rf/reg-event-db
 ::save-popover-data
 (fn [db [_ id]]
   (let [data @(rf/subscribe [::subs/get-field [:form id]])
           error-fields (reduce-kv (fn [m k v]
                                     (if-not (:valid? v)
                                       (conj m v) m))
                                   [] data )]
       (if (empty? error-fields)
         ())
       #_(print  valid)
       db ) db) )

(rf/reg-event-fx
 ::discard-data
 (fn [db]
   {:dispatch-n  (list [::reset-dialog]
                       [::close])}))

(rf/reg-event-db
 ::reset-dialog
 (fn [db _]
   (update-in db  [:dialog :edit-pyrometer] dissoc :form :data :field)))
