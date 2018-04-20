;; subscriptions for dialog custom-emissivity
(ns tta.dialog.custom-emissivity.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.component.settings.subs :as src-subs]
            [tta.app.subs :as app-subs]
            [tta.util.common :as au]))

;; primary signals

(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :custom-emissivity])))

(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _] (:open? dialog)))

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
  ::view
  :<- [::dialog]
  (fn [dialog _]
    (:view dialog)))
;;structure of src data : arranged as array of levels
;;
;; {:levels [{:tube-rows [{:custom-emissivity [[side x tube]]}
;;                        ..]}
;;            ..]}
;;
;; in case of side-fired, it is transformed into a single virtual level
;; so as homogenize the structure and keep the logic simple in this component


(rf/reg-sub
 ::src-data
 :<- [::src-subs/data]
 :<- [::app-subs/plant]
 (fn [[settings plant] _]
   (let [{:keys [config]} plant
         {:keys [firing]} config
         [level-keys row-count tube-counts]
         (case firing
           "side" (let [chs (get-in config [:sf-config :chambers])]
                    [[:reformer]
                     (count chs)
                     (map :tube-count chs)])
           "top" [(->> (get-in config [:tf-config :measure-levels])
                       (filter val)
                       (keys)
                       (map {:top? :top
                              :middle? :middle
                              :bottom? :bottom}))
                  (get-in config [:tf-config :tube-row-count])
                  (map :tube-count (get-in config [:tf-config :tube-rows]))])]
     {:levels
      (->>
        (or (case firing
              "side" {:reformer {:tube-rows (get-in settings [:sf-settings :chambers])}}
              "top" (get-in settings [:tf-settings :levels]))
            (map #(list % {}) level-keys))
        (map (fn [[key level]]
               {key
                {:tube-rows
                 (->> (or (:tube-rows level)
                          (repeat row-count {}))
                      (mapv (fn [tube-count row]
                              {:custom-emissivity
                               (or (:custom-emissivity row)
                                   (vec (repeat 2 (vec (repeat tube-count nil)))))})
                            tube-counts))}}))
        (into {}))})))

(rf/reg-sub
 ::data
 :<- [::dialog]
 :<- [::src-data]
 (fn [[dialog src-data] _]
   (or (:data dialog) src-data)))

(rf/reg-sub
 ::form
 :<- [::dialog]
 (fn [dialog _] (:form dialog)))

(rf/reg-sub
 ::field
 :<- [::form]
 :<- [::data]
 (fn [[form data] [_ path]]
   (or (get-in form path)
       {:value (get-in data path)
        :valid? true})))

(rf/reg-sub-raw
  ::level-opts ;; bottom tab selection options
  (fn [_ _]
    (reaction
      (let [config (:config @(rf/subscribe [::app-subs/plant]))]
        (case (:firing config)
          "side" [{:id :reformer
                   :label (translate [:data-entry :levels :reformer] "Reformer")}]
          (->> [{:id :top
                 :label (translate [:data-entry :levels :top] "Top")
                 :show? (get-in config [:tf-config :measure-levels :top?])}
                {:id :middle
                 :label (translate [:data-entry :levels :middle] "Middle")
                 :show? (get-in config [:tf-config :measure-levels :middle?])}
                {:id :bottom
                 :label (translate [:data-entry :levels :bottom] "Bottom")
                 :show? (get-in config [:tf-config :measure-levels :bottom?])}]
               (filter :show?)
               (vec)))))))

(rf/reg-sub
  ::selected-level ;; bottom tab selected
  :<- [::view]
  (fn [view _] (or (:selected-level view) 0)))

(rf/reg-sub
  ::selected-level-key
  :<- [::selected-level]
  :<- [::level-opts]
  (fn [[index opts] _] (get-in opts [index :id])))

(rf/reg-sub
  ::level-key
  :<- [::level-opts]
  (fn [opts [_ index]] (get-in opts [index :id])))

(rf/reg-sub
 ::tube-pref
 :<- [::src-subs/data]
 (fn [settings [_ row index]]
   (or (get-in settings [:tf-settings :tube-rows row :tube-prefs index])
       (get-in settings [:sf-settings :chambers row :tube-prefs index]))))
