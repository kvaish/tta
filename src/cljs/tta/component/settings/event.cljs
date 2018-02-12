;; events for component setting
(ns tta.component.settings.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.component.settings.subs :as subs]))

;; Add some event handlers, like
#_ (rf/reg-event-db
    ::event-id
    (fn [db [_ param]]
      (assoc db :param param)))
;;
;; NOTE: all event handler functions should be pure functions
;; Typically rf/reg-event-db should suffice for most cases, which
;; means you should not access or modify any global vars or make
;; external service calls.
;; If external data/changes needed use rf/reg-event-fx, in which case
;; your event handler function should take a co-effects map and return
;; a effects map, like
#_ (rf/reg-event-fx
    ::event-id
    (fn [{:keys[db]} [_ param]]
      {:db (assoc db :param param)}))
;;
;; If there is a need for external data then inject them using inject-cofx
;; and register your external data sourcing in cofx.cljs
;; Similarly, if your changes are not limited to the db, then use
;; rf/reg-event-fx and register your external changes as effects in fx.cljs

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
 (fn [db [_ path value validations]]
   (let [data @(rf/subscribe [::subs/data])
         field (make-field value)
         validated-field
         (-> field
             (required validations)
             (number validations))

         new-data (if (:valid? validated-field)
                    (assoc-in data path value)
                    data)]
     (-> db
         (assoc-in
          (into [:component :settings :form] path)
          validated-field)
         (assoc-in [:component :settings :data] new-data)))))
