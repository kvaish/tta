;; events for component config
(ns tta.component.config.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]
            [tta.component.config.subs :as subs]
            [clojure.string :as str]))

(def form-path [:component :config :form])
(def db-path [:component :config :data])

(defn validate-required [field]
  (if (empty? (:value field))
    (assoc field :valid? false :error "This field is required.")
    field))

(defn validate-number [field]
  (if (and (:valid? field)
           (not (re-matches #"\d+" (:value field))))
    (assoc field
      :error "Please enter a number only"
      :valid? false)
    ;; pass through
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

(defn tbsc [key]
  (let [data @(rf/subscribe [::subs/chambers [0]])
        seq (select-keys data [:tube-count :burner-count-per-row
                               :peep-door-count :section-count])]
    (get seq key)))

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

(defn validate-tc [field]
  (let [sc (tbsc :section-count)]
    (if (nil? sc)
      field
      (if (zero? (mod (:value field) sc))
        field
        (assoc field
          :valid? false
          :error "no of tubes must be multiple of no of sections!")))))

(defn validate-tb-less-than-pc
  [field]
  (let [tc (tbsc :tube-count)
        pc (tbsc :peep-door-count)]
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
          :error "no of tubes must be higher than no of peep doors!")))))

(defn validate-pc
  [field]
  (let [sc (tbsc :section-count)]
    (if (nil? sc)
      field
      (if (zero? (mod (:value field) sc))
        field
        (assoc field
          :valid? false
          :error "no of peepdoors must be multiple of no of sections!")))))

(defn validate-bc
  [field]
  (let [sc (tbsc :section-count)]
    (if (nil? sc)
      field
      (if (zero? (mod (:value field) sc))
        field
        (assoc field
          :valid? false
          :error "no of burners per row must be multiple of no of sections!")))))

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

(defn parse-number [field]
  (let [{:keys [value valid?]} field]
    (if valid?
      (assoc field :value (js/Number value))
      field)))

(defn get-tbp [firing]
  (let [data @(rf/subscribe [::subs/config])
        d (case firing
            "side" (vector
                     (get-in data [:chambers 0 :tube-count])
                     (get-in data [:chambers 0 :burner-row-count])
                     (get-in data [:chambers 0 :peep-door-count]))
            "top" (vector
                    (get-in data [:tf-config :tube-row-count])
                    (count (get-in data [:tf-config :burner-rows]))))]
    d))

(defn distribute [n p]
  (let [q (quot n p) r (rem n p) e (quot r 2)
        s (- r e) v (repeat p q) p2 (- p r) v2 (drop s v)]
    (if (= r 0)
      v
      (concat
        (map inc (take s v))
        (take p2 v2)
        (map inc (drop p2 v2))))))

(rf/reg-event-db
  ::close
  (fn [db _]
    (-> db
        (assoc-in form-path nil)
        (assoc-in db-path nil))))

(rf/reg-event-db
  ::set-pdt-rows
  (fn [db _]
    (let [data @(rf/subscribe [::subs/data])
          tc (get-in data [:sf-config :chambers 0 :tube-count])
          sc (get-in data [:sf-config :chambers 0 :section-count])
          pc (get-in data [:sf-config :chambers 0 :peep-door-count])
          n-ch @(rf/subscribe [::subs/ch-count])
          v (distribute (/ tc sc) (/ pc sc))
          tpsc (vec (flatten (repeat pc v)))
          form-data (mapv #(make-field %) tpsc)
          new-data (update-in data [:sf-config :chambers]
                              (fn [chs]
                                (mapv (fn [ch]
                                        (assoc-in ch [:peep-door-tube-count] tpsc))
                                      (or chs (repeat n-ch {})))))]
      (-> db
          (assoc-in
            (into form-path [:sf-config :peep-door-tube-count])
            form-data)
          (assoc-in db-path new-data)))))

(rf/reg-event-db
  ::set-tbps-tf
  (fn [db _]
    (let [data @(rf/subscribe [::subs/data])
          tc (get-in data [:tf-config :tube-rows 0 :tube-count])
          bc (get-in data [:tf-config :burner-rows 0 :burner-count])
          sc (get-in data [:tf-config :section-count])
          t-dist (distribute tc sc)
          b-dist (distribute bc sc)
          tb-dist (map #(conj {:tube-count %1}
                              {:burner-count %2})
                       t-dist b-dist)
          t-form-data (mapv #(make-field %) t-dist)
          b-form-data (mapv #(make-field %) b-dist)
          form-data (mapv #(conj {:tube-count %1}
                             {:burner-count %2})
                          t-form-data b-form-data)
          new-data (assoc-in data [:tf-config :sections] tb-dist)]
      (-> db
          (assoc-in
            (into form-path [:tf-config :sections])
            form-data)
          (assoc-in db-path new-data)))))

(rf/reg-event-fx
  ::set-section-count-tf
  (fn [{:keys [db]} [_ path value]]
    (let [field (make-field value)
          data @(rf/subscribe [::subs/data])
          d (get-tbp (:firing data))
          validated-field
          (cond-> field
                  true (validate-required)
                  (nil? (:error field)) (validate-number)
                  (nil? (:error field)) (validate-sc d))
          validated-field (parse-number validated-field)
          new-data
          (if (:valid? validated-field)
            (assoc-in data [:tf-config :section-count]
                      (:value validated-field))
            data)]
      {:db (-> db
               (assoc-in
                 (into form-path [:tf-config :section-count])
                 validated-field)
               (assoc-in db-path new-data))
       :dispatch [::set-tbps-tf]})))

(rf/reg-event-fx
  ::set-ch-common-field-sf
  (fn [{:keys [db]} [_ path value validations]]
    (let [data @(rf/subscribe [::subs/data])
          n-ch @(rf/subscribe [::subs/ch-count])
          field (make-field value)
          validated-field
          (cond-> field
                  true (validate-required)
                  true (validate-number))
          validated-field (parse-number validated-field)
          new-data (if (:valid? validated-field)
                     (update-in data [:sf-config :chambers]
                                (fn [chs]
                                  (mapv (fn [ch]
                                          (assoc-in ch path (:value validated-field)))
                                        (or chs (repeat n-ch {})))))
                     data)]
      {:db (-> db
               ;(update-in sf-config-form-path assoc-in path validated-field)
               (assoc-in
                 (into form-path (flatten [:sf-config path]))
                 validated-field)
               (assoc-in db-path new-data))
       :dispatch [::set-pdt-rows]})))

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
                              (validate-tpsc index pdt-data tc sc)
                              (validate-required)
                              (validate-number))
          validated-field (parse-number validated-field)
          new-pdt-data (vec
                         (flatten
                           (repeat sc
                                   (assoc pdt-data index (:value validated-field)))))
          new-data (if (:valid? validated-field)
                     (update-in data [:sf-config :chambers]
                                (fn [chs]
                                  (mapv (fn [ch]
                                          (assoc-in ch [:peep-door-tube-count] new-pdt-data))
                                        (or chs (repeat n-ch {})))))
                     data)]
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
                      (vec (repeat 2 (first chs)))
                      (pop chs)))
          new-data (-> data
                       (assoc-in [:sf-config :chambers] new-chs)
                       (assoc-in [:sf-config :placement-of-WHS] "end"))]
      (-> db
          (assoc-in
            (into form-path path)
            field)
          (assoc-in db-path new-data)))))

(rf/reg-event-db
  ::update-burner-rows
  (fn [db _]
    (let [data @(rf/subscribe [::subs/data])
          trc (get-in data [:tf-config :tube-row-count])
          bf? (get-in data [:tf-config :burner-first?])
          brs (get-in data [:tf-config :burner-rows])
          brc (if bf?
                (inc trc)
                (dec trc))
          field (make-field brc)
          new-data (-> data
                       (assoc-in [:tf-config :burner-row-count] brc)
                       (assoc-in [:tf-config :burner-rows]
                                 (repeat brc (or (first brs) {}))))]
      (-> db
          (assoc-in
            (into form-path [:tf-config :burner-row-count])
            field)
          (assoc-in db-path new-data)))))

(rf/reg-event-fx
  ::set-burner-first
  (fn [{:keys [db]} [_ path value]]
    (js/console.log path value)
    (let [field (make-field value)
          data @(rf/subscribe [::subs/data])
          new-data (assoc-in data [:tf-config :burner-first?] value)]
      {:db (-> db
               (assoc-in
                 (into form-path path)
                 field)
               (assoc-in db-path new-data))
       :dispatch [::update-burner-rows]})))

(rf/reg-event-fx
  ::set-tube-row-count
  (fn [{:keys [db]} [_ path value validations]]
    (let [field (make-field value)
          data @(rf/subscribe [::subs/data])
          validated-field
          (cond-> field
                  (:required? validations) (validate-required)
                  (:number validations) (validate-number)
                  ;;other validations
                  )
          validated-field (if (:number validations)
                            (parse-number validated-field)
                            validated-field)
          new-data (assoc-in data [:tf-config :tube-row-count]
                             (:value validated-field))]
      {:db (-> db
               (assoc-in
                 (into form-path path)
                 validated-field)
               (assoc-in db-path new-data))
       :dispatch [::update-burner-rows]})))

(rf/reg-event-fx
  ::set-tb-rows-tf
  (fn [{:keys [db]} [_ key value validations]]
    (let [field (make-field value)
          data @(rf/subscribe [::subs/data])
          tf-config (:tf-config data)
          [k2 rc rows] (case key
                         :tube-count [:tube-rows
                                      (:tube-row-count tf-config)
                                      (:tube-rows tf-config)]
                         :burner-count [:burner-rows
                                        (:burner-row-count tf-config)
                                        (:burner-rows tf-config)])
          validated-field (-> field
                              (validate-required)
                              (validate-number))
          validated-field (parse-number validated-field)
          form-data (repeat (count rows) {key validated-field})
          new-data (if (:valid? validated-field)
                     (update-in data [:tf-config k2]
                                (fn [rows]
                                  (mapv (fn [row]
                                          (assoc-in row [key] (:value validated-field)))
                                        (or rows (repeat rc {})))))
                     data)]

      {:db (-> db
               (assoc-in
                 (into form-path [:tf-config k2])
                 form-data)
               (assoc-in db-path new-data))
       :dispatch [::set-tbps-tf]})))

(rf/reg-event-db
  ::set-start-end-options
  (fn [db [_ path value]]
    (js/console.log path value)
    (let [value (:id value)
          data @(rf/subscribe [::subs/data])
          field (make-field value)
          v (if (re-matches #".*→.*" value)
              (str/split value #"→")
              (str/split value #"←"))
          key (last path)
          index (nth (reverse path) 1)
          [s e r] (case key
                  :tube-count [:start-tube :end-tube :tube-rows]
                  :burner-count [:start-burner :end-burner :burner-rows])
          [v1 v2 v3] [(js/Number (get v 0))
                      (js/Number (get v 1))
                      @(rf/subscribe [::subs/config-field-tf [r index key]])]
          new-data (-> data
                       (assoc-in (into [] (concat (pop path) [s])) v1)
                       (assoc-in (into [] (concat (pop path) [e])) v2))
          [path form-data] (case (:firing data)
                             "side" [path field]
                             "top" [(pop path) {key v3
                                                s   (make-field v1)
                                                e   (make-field v2)}])]
      (js/console.log path form-data new-data)
      (-> db
          (assoc-in
            (into form-path path)
            form-data)
          (assoc-in db-path new-data)))))

(rf/reg-event-db
  ::set-field
  (fn [db [_ path value validations]]
    (js/console.log path value validations)
    (let [data @(rf/subscribe [::subs/data])
          field (make-field value)
          validated-field
          (cond-> field
                  (:required? validations) (validate-required)
                  (:number validations) (validate-number)
              ;;other validations
              )
          validated-field (if (:number validations)
                            (parse-number validated-field)
                            validated-field)
          new-data (if (:valid? validated-field)
                     (assoc-in data path (:value validated-field))
                     data)]
      (js/console.log validated-field new-data)
      (-> db
          (assoc-in
            (into form-path path)
            validated-field)
          (assoc-in db-path new-data)))))

