;; subscriptions for component config
(ns tta.component.config.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :as rr]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]
            [tta.util.common :as au]))

(defn get-field
  ([path form data] (get-field path form data identity))
  ([path form data parse]
   (or (get-in form path)
       {:value (parse (get-in data path))
        :valid? true})))

(rf/reg-sub
  ::src-data
  :<- [::app-subs/plant]
  (fn [plant _] (:config plant)))

(rf/reg-sub
 ::component
 (fn [db _]
   (or (get-in db [:component :config])
       (get-in db [:plant :config]))))

(rf/reg-sub
  ::dirty?
  :<- [::data]
  :<- [::src-data]
  (fn [[data src-data] _] (not= data src-data)))

(rf/reg-sub
  ::valid?
  :<- [::form]
  (fn [form _] (not (au/some-invalid form))))

(rf/reg-sub
  ::can-submit?
  :<- [::dirty?]
  :<- [::valid?]
  (fn [[dirty? valid?] _] (and dirty? valid?)))

(rf/reg-sub
  ::warn-on-close?
  :<- [::dirty?]
  :<- [::valid?]
  (fn [[dirty? valid?] _] (or dirty? (not valid?))))

(rf/reg-sub
  ::data
  :<- [::src-data]
  :<- [::component]
  (fn [[src-data component] _]
    (or (:data component)
        src-data)))

(rf/reg-sub
  ::form
  :<- [::component]
  (fn [component _] (:form component)))

(rf/reg-sub
  ::field
  :<- [::form]
  :<- [::data]
  (fn [[form data] [_ path]] (get-field path form data)))

(rf/reg-sub
  ::firing
  :<- [::form]
  :<- [::data]
  (fn [[form data] _]
    (get-field [:firing] form data)))

(rf/reg-sub
  ::firing-opts
  (fn []
    [{:id "side" :name "Side fired"}
     {:id "top" :name "Top fired"}]))

(rf/reg-sub
  ::config
  :<- [::data]
  (fn [data [_ path]]
    (case (:firing data)
      "side" (get-in data (into [] (concat [:sf-config] path)))
      "top"  (get-in data (into [] (concat [:tf-config] path))))))

(defn make-field [v]
  {:value v
   :valid? true})

(rf/reg-sub
  ::config-field-tf
  :<- [::form]
  :<- [::config]
  (fn [[form config] [_ path]]
    (or
      (get-in form (into [:tf-config] path))
      (make-field (get-in config path)))))

(rf/reg-sub
  ::tube-count-tf
  :<- [::form]
  :<- [::config]
  (fn [[form config] [_ path]]
    (or
      (get-in form [:tf-config :tube-count])
      (make-field (get-in config [:tube-rows 0 :tube-count])))))

(rf/reg-sub
  ::burner-count-tf
  :<- [::form]
  :<- [::config]
  (fn [[form config] [_ path]]
    (or
      (get-in form [:tf-config :burner-count])
      (make-field (get-in config [:burner-rows 0 :burner-count])))))

(rf/reg-sub
  ::chambers
  :<- [::config]
  (fn [config [_ path]]
    (get-in config (into [] (concat [:chambers] path)))))

(rf/reg-sub
  ::ch-fields
  :<- [::form]
  :<- [::chambers]
  (fn [[form chambers] [_ path]]
    (or
      (get-in form (into [] (concat [:sf-config :chambers] path)))
      (make-field (get-in chambers path)))))

(rf/reg-sub
  ::ch-common-field
  :<- [::form]
  :<- [::config]
  (fn [[form config] [_ path]]
    (or
      (get-in form (into [] (concat [:sf-config] path)))
      (make-field (get-in config (into [] (concat [:chambers 0] path)))))))

(rf/reg-sub
  ::ch-count
  :<- [::chambers]
  (fn [chambers _]
    (count chambers)))

(rf/reg-sub
  ::pdt-count
  :<- [::form]
  :<- [::chambers]
  (fn [[form chambers] _]
    (let [ptc (get-in chambers [0 :peep-door-tube-count])
          pc (count ptc)
          sc (get-in chambers [0 :section-count])
          psc (/ pc sc)
          form-data (:value (get-in form [:sf-config :peep-door-tube-count]))]
      (or
        (if form-data
          (take psc form-data))
        (into [] (map #(make-field %) (take psc ptc)))))))

(rf/reg-sub
  ::start-end-options
  :<- [::config]
  :<- [::firing]
  :<- [::burner-count-tf]
  :<- [::tube-count-tf]
  (fn [[config firing bc tc] [_ key]]
    (let [[k1 v1] (if (= key :tube-row-count)
                    [:tube-rows (:value tc)]
                    [:burner-rows (:value bc)])
          [c rc]
          (case (:value firing)
            "side" [(get-in config [:chambers 0 key])
                    (count (:chambers config))]
            "top" [v1 (count (k1 config))])
          starts (map #(inc (* % c)) (range rc))
          ends (map #(* (inc %) c) (range rc))
          opts (into
                 (map #(str %1 "→" %2) starts ends)
                 (map #(str %1 "←" %2) ends starts))]
      (reduce (fn [coll v]
                (conj coll {:id   v
                            :name v})) [] opts))))

(rf/reg-sub
  ::selected-start-end
  :<- [::config]
  :<- [::firing]
  (fn [[config firing] [_ path key]]
    (let [[k1 k2] (case key
                    :tube-count [:start-tube :end-tube]
                    :burner-count [:start-burner :end-burner])
          [s e] (case (:value firing)
                  "side" [(get-in config (into [] (concat [:chambers] path [k1])))
                          (get-in config (into [] (concat [:chambers] path [k2])))]
                  "top" [(get-in config (into path [k1]))
                         (get-in config (into path [k2]))])]
      (js/console.log s e)
      (if (< s e)
        (str s "→" e)
        (str s "←" e)))))

(rf/reg-sub
  ::section-rows-tf
  :<- [::config-field-tf]
  (fn [config-field-tf [_ index key]]
    (get-in config-field-tf [:sections index key])))