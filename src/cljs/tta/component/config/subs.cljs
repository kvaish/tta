;; subscriptions for component config
(ns tta.component.config.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :as rr]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

;; primary signals

(rf/reg-sub
  ::config
  (fn [db _]
    (get-in db [:plant :config])))

(rf/reg-sub
 ::component
 (fn [db _]
   (or (get-in db [:component :config])
       (get-in db [:plant :config]))))

;; derived signals/subscriptions

(rf/reg-sub
  ::data
  :<- [::config]
  :<- [::component]
  (fn [[config component] _]
    (or (:data component)
        config)))

(rf/reg-sub
  ::field
  :<- [::data]
  :<- [::component]
  (fn [[data component] [_ path]]
    (let [init-field (get-in data path)
          form-field (get-in (:form component) path)]
      (or (:value form-field)
          init-field))))

(rf/reg-sub
  ::firing
  :<- [::data]
  (fn [data _]
    (:firing data)))

(rf/reg-sub
  ::config-data
  :<- [::data]
  (fn [data [_ path]]
    (case (:firing data)
      "side" (get-in data (into [] (concat [:sf-config] path)))
      "top"  (get-in data (into [] (concat [:tf-config] path))))))

(rf/reg-sub
  ::chambers
  :<- [::config-data]
  (fn [config-data [_ path]]
    (get-in config-data (into [] (concat [:chambers] path)))))

(rf/reg-sub
  ::ch-count
  :<- [::chambers]
  :<- [::component]
  (fn [[chambers component] _]
    (or (count chambers)
        (case (get-in component [:form :dual-chamber? :value])
          true 2
          false 1))))

(rf/reg-sub
  ::pdt-count
  :<- [::chambers]
  :<- [::component]
  (fn [[chambers component] _]
    (let [ptc (get-in chambers [0 :peep-door-tube-count])
          pc (count ptc)
          sc (get-in chambers [0 :section-count])
          psc (/ pc sc)]
      (vec (take psc ptc))
      #_(or (:value form-field)
          data))))

(rf/reg-sub
  ::start-end-options
  :<- [::config-data]
  :<- [::firing]
  (fn [config-data firing [_ path key]]
    (let [c (get-in config-data path)
          r (case firing
              "side" (case (count (:chambers config-data))
                       1 []
                       2 [])
              "top" )])))