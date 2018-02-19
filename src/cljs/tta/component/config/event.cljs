;; events for component config
(ns tta.component.config.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.component.config.subs :as subs]
            [clojure.string :as str]))

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

(def form-path [:component :config :form])
(def db-path [:component :config :data])

(defn validate-required [field]
  (if (empty? (:value field))
    (assoc field :valid? false :error "This field is required.")
    field))

(defn validate-number [field]
  (if (number? (js/Number (:value field)))
    field))

(defn validate-min [field min-val]
  (if (< (:value field) (or min-val 0))
    (assoc field :valid? false :error
                 (str "Value should be greater than " min-val))
    field))

(defn validate-max [field max-val]
  (if (> (:value field) (or max-val 10000))
    (assoc field :valid? false :error
                 (str "Value can not exceed " max-val))
    field))

(defn validate-sc
  "d [tube burner peepdoors]"
  [field d]
  (if (not-every?
        zero?
        (map #(mod % (:value field)) d))
    (assoc field
      :valid? false
      :error "no of tubes, no of burners per row, no of peepdoors must be multiple of no of sections!")
    field))

(defn validate-tc
  [field sc]
  (if (nil? sc)
    field
    (if (zero? (mod (:value field) sc))
      field
      (assoc field
        :valid? false
        :error "no of tubes must be multiple of no of sections!"))))

(defn validate-tc-less-than-pc
  "send tc as nil and value of pc if validating for tc and vice versa"
  [field tc pc]
  (if tc
    (if (> tc (:value field))
      field
      (assoc field
        :valid? false
        :error "no of tubes must be higher than no of peep doors!"))
    (if (< pc (:value field))
      field
      (assoc field
        :valid? false
        :error "no of tubes must be higher than no of peep doors!"))))

(defn validate-pc
  [field sc]
  (if (nil? sc)
    field
    (if (zero? (mod (:value field) sc))
      field
      (assoc field
        :valid? false
        :error "no of peepdoors must be multiple of no of sections!"))))

(defn validate-bc
  [field sc]
  (if (nil? sc)
    field
    (if (zero? (mod (:value field) sc))
      field
      (assoc field
        :valid? false
        :error "no of burners per row must be multiple of no of sections!"))))

(defn validate-tpsc
  [field i pdtc tc sc]
  (let [tps (/ tc sc)
        new-pdtc (assoc pdtc i (:value field))
        sum (reduce + new-pdtc)]
    (if (= tps sum)
      field
      (assoc field
        :valid? false
        :error (str "sum of no of tubes for each peep
        door in one section must be equal to
        no of tubes in one section:" tps)))))

(defn make-field [value]
  {:valid? true
   :error nil
   :value value})

(defn get-tbp [firing]
  (let [data @(rf/subscribe [::subs/config-data])
        d (case firing
            "side" (vector
                     (get-in data [:chambers 0 :tube-count])
                     (get-in data [:chambers 0 :burner-row-count])
                     (get-in data [:chambers 0 :peep-door-count]))
            "top" (vector
                    (get-in data [:tf-config :tube-row-count])
                    (count (get-in data [:tf-config :burner-rows]))))]))

(rf/reg-event-db
  ::set-section-count
  (fn [db [_ path value validations]]
    (let [field (make-field value)
          data (@rf/subscribe [::subs/data])
          d (get-tbp (:firing data))
          validated-field
          (-> field
              (validate-required)
              (validate-number)
              (validate-sc d))
          new-data (if (:valid? validated-field)
                     (assoc-in data path value)
                     data)]
      (-> db
          (assoc-in
            (into form-path path)
            validated-field)
          (assoc-in db-path new-data))
      ;;update peep-door-tube-count-data
      )))

(rf/reg-event-db
  ::set-ch-common-field-sf
  (fn [db [_ path value validations]]
    (let [data @(rf/subscribe [::subs/data])
          n-ch @(rf/subscribe [::subs/ch-count])
          field (make-field value)
          validated-field
          (cond-> field
                  (:required? validations) (validate-required)
                  (contains? validations :number) (validate-number)
                  ;;other validations
                  )
          new-data (if (:valid? validated-field)
                     (update-in data [:sf-config :chambers]
                                (fn [chs]
                                  (mapv (fn [ch]
                                          (assoc-in ch path value))
                                        (or chs (repeat n-ch {})))))
                     data)]
      (-> db
          (assoc-in
            (into form-path path)
            validated-field)
          (assoc-in db-path new-data)))))

(rf/reg-event-db
  ::set-pdt-count-sf
  (fn [db [_ path value]]
    (let [data @(rf/subscribe [::subs/data])
          n-ch @(rf/subscribe [::subs/ch-count])
          tc @(rf/subscribe [::subs/chambers [0 :tube-count]])
          pc @(rf/subscribe [::subs/chambers [0 :peep-door-count]])
          sc @(rf/subscribe [::subs/chambers [0 :section-count]])
          pdt-data @(rf/subscribe [::subs/pdt-count])
          index (peek path)
          psc (/ pc sc)
          field (make-field value)
          validated-field (-> field
                              (validate-required)
                              (validate-number)
                              (validate-tpsc index pdt-data tc sc))
          new-pdt-data (vec
                         (flatten
                           (repeat sc
                                   (assoc pdt-data index (:value field)))))
          new-data (if (:valid? validated-field)
                     (update-in data [:sf-config :chambers]
                                (fn [chs]
                                  (mapv (fn [ch]
                                          (assoc-in ch (pop path) new-pdt-data))
                                        (or chs (repeat n-ch {}))))))]
      (-> db
          (assoc-in
            (into form-path path)
            validated-field)
          (assoc-in db-path new-data)))))

(rf/reg-event-db
  ::set-dual-chamber
  (fn [db [_ path value]]
    (let [field (make-field value)
          data @(rf/subscribe [::subs/data])
          chs @(rf/subscribe [::subs/chambers])
          new-chs (if (nil? chs)
                    (if (true? value)
                      (into [] (repeat 2 {}))
                      [{}])
                    (if (true? value)
                      (assoc chs 1 (0 chs))
                      (pop chs)))]
      (-> db
          (assoc-in
            (into form-path path)
            field)
          (assoc-in db-path new-chs))
      ;;update start-end t,b
      )))

(rf/reg-event-db
  "key can be either tube or burner for tf"
  ::set-tb-rows-tf
  (fn [db [_ path value key]]
    (let [field (make-field value)
          data @(rf/subscribe [::subs/data])
          [rc rows] (case key
               "tube" [@(rf/subscribe
                          [::subs/config-data [:tube-row-count]])
                       @(rf/subscribe
                          [::subs/config-data [:tube-rows]])]
               "burner" [@(rf/subscribe
                            [::subs/config-data [:burner-row-count]])
                         @(rf/subscribe
                            [::subs/config-data [:burner-rows]])])
          validated-field (-> field
                              (validate-required)
                              (validate-number))
          new-data (if (:valid? validated-field)
                     (if (nil? rows)
                       (into []
                             (repeat
                               rc
                               {path (:value field)}))
                       (mapv
                         #(assoc-in
                            % path
                            (:value field)) rows)))]
      (-> db
          (assoc-in
            (into form-path path)
            field)
          (assoc-in db-path new-data)))))

#_(rf/reg-event-db
  ::set-sections-tf
  (fn [db [_ path value]]
    (let [field (make-field value)
          data @(rf/subscribe [::subs/data])
          ])))

(rf/reg-event-db
  ::set-field
  (fn [db [_ path value validations]]
    (let [data @(rf/subscribe [::subs/data])
          field (make-field value)
          validated-field
          (cond-> field
                  (:required? validations) (validate-required)
                  (contains? validations :number) (validate-number)
              ;;other validations
              )
          new-data (if (:valid? validated-field)
                     (assoc-in data path value)
                     data)]
      (-> db
          (assoc-in
            (into form-path path)
            validated-field)
          (assoc-in db-path new-data)))))

