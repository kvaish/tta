;; events for component setting
(ns tta.component.settings.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.common :as au]
            [tta.app.event :as app-event]
            [tta.component.settings.subs :as subs]))

(defn make-field [value]
  {:valid? true
   :error nil
   :value value})

(defn missing-field []
  {:value nil, :valid? false
   :error (translate [:validation :hint :required]
                     "* Required field")})

(def data-path [:component :settings :data])
(def form-path [:component :settings :form])

;; validation mulit-method (fn validate [field params])
;; each one will check further only if :valid? is true, otherwise skip it
(defmulti validate #(:type %2))

(defn validate-field [field & validators]
  (reduce #(validate %1 %2) field (remove nil? validators)))

(defmethod validate :number
  [{:keys [value valid?] :as field} _]
  (if (and valid?
           (not (re-matches #"\d+" value)))
    (assoc field
           :error (translate [:validation :hint :number]
                             "Please enter a number only")
           :valid? false)
    ;; pass through
    field))

(defmethod validate :max-number
  [{:keys [value valid?] :as field} {:keys [max]}]
  (if (and valid? (> value max))
    (assoc field
           :error (translate [:validation :hint :max-number]
                             "Please enter a number smaller than {max}"
                             {:max max})
           :valid? false)
    ;; pass through
    field))

(defmethod validate :min-number
  [{:keys [value valid?] :as field} {:keys [min]}]
  (if (and valid? (< value min))
    (assoc field
           :error (translate [:validation :hin :min-number]
                             "Please enter a number greater than {min}"
                             {:min min})
           :valid? false)
    ;; pass through
    field))

;; parse mulit-method (fn parse [field params])
;; parse method only checks the :valid? key, which when true implies
;; that the :value is in correct shape for parsing
(defmulti parse #(:type %2))

(defn parse-value [field & parsers]
  (reduce #(parse %1 %2) field (remove nil? parsers)))

(defmethod parse :number
  [{:keys [value valid?] :as field} _]
  (if valid?
    (assoc field :value (js/Number value))
    field))

(defmethod parse :temp
  [{:keys [value valid?] :as field} {:keys [temp-unit]}]
  (if valid?
    (assoc field :value (au/from-temp-unit value temp-unit))
    field))

(defn treat-blank [required?]
  (if required?
    ;; show message but do not clear data
    [false nil, true (missing-field)]
    ;; optional! clear both data and form
    [true nil true nil]))

(rf/reg-event-fx
 ::init
 (fn [_ _]
   (let [pid (:value @(rf/subscribe [::subs/field [:pyrometer-id]]))]
     {:dispatch [::set-field [:pyrometer-id] pid true]})))

(rf/reg-event-db
 ::close
 (fn [db _]
   (-> db
       (assoc-in form-path nil)
       (assoc-in data-path nil))))

(rf/reg-event-fx
 ::upload
 (fn [{:keys [db]} [_ next-event]]
   ;;TODO: save and then dispatch next-event
   (js/console.log "todo: upload settings")
   (if next-event {:dispatch next-event})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])]
     (cond-> db
       :always (assoc-in data-path (assoc-in data path value))
       required? (update-in form-path assoc-in path (if (some? value)
                                                      (make-field value)
                                                      (missing-field)))))))

(rf/reg-event-fx
 ::set-temp-unit
 (fn [{:keys [db]} [_ %]]
   (let [data @(rf/subscribe [::subs/data])
         target-temp (subs/get-field-temp [:target-temp] nil data %)
         design-temp (subs/get-field-temp [:design-temp] nil data %)]
     {:dispatch [::set-field [:temp-unit] %]
      :db (update-in db form-path
                     #(-> %
                          (assoc-in [:target-temp] target-temp)
                          (assoc-in [:design-temp] design-temp)))})))

(rf/reg-event-db
 ::set-temp
 (fn [db [_ path value required?]]
   (let [data @(rf/subscribe [::subs/data])
         temp-unit @(rf/subscribe [::subs/temp-unit])
         [d? value f? field]
         (if-let [value (not-empty value)]
           ;; has value, check and update
           (let [f (validate-field (make-field value) {:type :number})
                 v (parse-value f {:type :number}
                                {:type :temp, :temp-unit temp-unit})]
             ;; update data when valid and block typing when invalid number
             [(:valid? v) (:value v) (:valid? f) f])
           ;; blank!
           (treat-blank required?))]
     (cond-> db
       f? (update-in form-path assoc-in path field)
       d? (assoc-in data-path (assoc-in data path value))))))

(rf/reg-event-db
 ::set-number
 (fn [db [_ path value required? {:keys [max min]}]]
   (let [data @(rf/subscribe [::subs/data])
         [d? value f? field]
         (if-let [value (not-empty value)]
           ;; has value, check and update
           (let [f (validate-field (make-field value) {:type :number})
                 f2 (validate-field f
                                    (if max {:type :max-number, :max max})
                                    (if min {:type :min-number, :min min}))
                 v (parse-value f2 {:type :number})]
             ;; update data when valid and block typing when invalid number
             [(:valid? v) (:value v) (:valid? f) f2])
           ;; blank!
           (treat-blank required?))]
     (cond-> db
       f? (update-in form-path assoc-in path field)
       d? (assoc-in data-path (assoc-in data path value))))))
