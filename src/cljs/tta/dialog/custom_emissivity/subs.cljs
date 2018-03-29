;; subscriptions for dialog custom-emissivity
(ns tta.dialog.custom-emissivity.subs
  (:require [re-frame.core :as rf]
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
         [level-count row-count tube-counts]
         (case firing
           "side" (let [chs (get-in config [:sf-config :chambers])]
                    [1
                     (count chs)
                     (map :tube-count chs)])
           "top" [(->> (get-in config [:tf-config :measure-levels])
                        (filter val)
                        (count))
                  (get-in config [:tf-config :tube-row-count])
                  (map :tube-count (get-in config [:tf-config :tube-rows]))])]
     {:levels
      (->>
       (or (case firing
             "side" [{:tube-rows (get-in settings [:sf-settings :chambers])}]
             "top" (get-in settings [:tf-settings :levels]))
           (repeat level-count {}))
       (mapv (fn [level]
               {:tube-rows
                (->> (or (:tube-rows level)
                         (repeat row-count {}))
                     (mapv (fn [tube-count row]
                             {:custom-emissivity
                              (or (:custom-emissivity row)
                                  (vec (repeat 2 (vec (repeat tube-count nil)))))})
                           tube-counts))})))})))

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
 ::selected-level-index
 :<- [::dialog]
 (fn [dialog _]
   (get-in dialog [:view :selected-level-index] 0)))

(rf/reg-sub
 ::field
 :<- [::form]
 :<- [::data]
 (fn [[form data] [_ path]]
   (or (get-in form path)
       {:value (get-in data path)
        :valid? true})))

(rf/reg-sub
 ::tab-opts
 :<- [::app-subs/plant]
 (fn [plant _]
   (let [firing (get-in plant [:config :firing])
         {:keys [measure-levels]} (get-in plant [:config :tf-config])
         levels [{:test :top?, :label "Top"}
                 {:test :middle?, :label "Middle"}
                 {:test :bottom?, :label "Bottom"}]]
     (case firing
       "side" ["Reformer"]
       "top" (->>
              (filter #(get measure-levels (:test %)) levels)
              (map :label))))))

(rf/reg-sub
 ::tube-pref
 :<- [::src-subs/data]
 (fn [settings [_ row index]]
   (or (get-in settings [:tf-settings :tube-rows row :tube-prefs index])
       (get-in settings [:sf-settings :chambers row :tube-prefs index]))))
