;; view elements dialog edit-pyrometer
(ns tta.dialog.edit-pyrometer.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [ht.util.interop :as i]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.core :as ui-core]
            [cljs-time.core :as t]
            [cljs-time.format :as tf]
            [ht.app.comp :as ht-comp]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :refer [scroll-box]]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.app.view :as app-view]
            [tta.dialog.edit-pyrometer.style :as style]
            [tta.dialog.edit-pyrometer.subs :as subs]
            [tta.dialog.edit-pyrometer.event :as event]))

(def date-formatter (tf/formatter "yyyy-MM-dd"))
(defn format-date [date] (tf/unparse date-formatter (t/date-time date)))

(defn prop-label [text]
  [:span {:style {:font-weight 300}} text ": "])

(defn prop-value [text]
  [:span {:style {:margin "0 24px 0 12px"}} text])

(defn form-field [label error widget]
  [:div (use-style style/form-field)
   [:span (use-sub-style style/form-field :label) label]
   widget
   [:span (use-sub-style style/form-field :error) error]])

(defmulti prop-field :type)

(defmethod prop-field :default [_]
  "N/A")

(defmethod prop-field :text [{:keys [label path]}]
  (let [{:keys [value error valid?]} @(rf/subscribe [::subs/po-field path])]
    (form-field
     label error
     [app-comp/text-input
      {:on-change #(rf/dispatch [::event/set-po-text path % true])
       :width 200
       :value value, :valid? valid?}])))

(defmethod prop-field :decimal [{:keys [label path max min precision]}]
  (let [{:keys [value error valid?]} @(rf/subscribe [::subs/po-field path])]
    (form-field
     label error
     [app-comp/text-input
      {:on-change #(rf/dispatch [::event/set-po-decimal path % true
                                 {:max max, :min min, :precision precision}])
       :value value, :valid? valid?}])))

(defmethod prop-field :date [{:keys [label path]}]
  (let [{:keys [value error valid?]} @(rf/subscribe [::subs/po-field path])]
    (form-field
     label error
     [:span "!!!"])))

(defn make-props []
  {:name {:type :text, :wide? true
          :label (translate [:pyrometer :name :label] "Name")}
   :serial-number {:type :text, :wide? true
                   :label (translate [:pyrometer :serial-number :label]
                                     "Serial number")}
   :wavelength {:type :decimal, :format #(str % " µm"), :precision 2
                :label (translate [:pyrometer :wavelength :label] "Wavelength")}
   :date-of-calibration {:type :date, :format format-date
                         :label (translate [:pyrometer :date-of-calibration :label]
                                           "Date of calibration")}
   :tube-emissivity {:type :decimal, :min 0, :max 1, :precision 2
                     :label (translate [:pyrometer :tube-emissivity :label]
                                       "Tube emissivity")}})

(defn item [pyro index]
  (let [props (make-props)]
    [ui/menu-item {:style {:border-radius "8px"}
                   :value index
                   :on-click #(rf/dispatch [::event/edit-pyrometer pyro index])}
     (into [:div (use-style style/menu-item)
            [:span {:style {:position "absolute"
                              :top 0, :right 0}}
             [app-comp/icon-button
              {:icon ic/delete
               :on-click #(do
                            (i/ocall % :preventDefault)
                            (i/ocall % :stopPropagation)
                            (rf/dispatch [::event/delete-pyrometer index]))}]]]
           (->> [[:name :serial-number]
                 [:wavelength :date-of-calibration :tube-emissivity]]
                (map (fn [ks]
                       (->> ks
                            (map #(list % (props %)))
                            (mapcat (fn [[k {:keys [label format]
                                            :or {format identity}}]]
                                      (list (prop-label label)
                                            (prop-value (format (get pyro k)))))))))
                (interleave (repeat '([:br])))
                (drop 1)
                (apply concat)))]))

(defn edit []
  (let [props (make-props)]
    (into [:div (use-style style/edit)
           [:div (use-sub-style style/edit :btns)
            [app-comp/icon-button-l
             {:icon ic/accept
              :disabled? (not @(rf/subscribe [::subs/po-can-submit?]))
              :on-click #(rf/dispatch [::event/save-pyrometer])}]
            [app-comp/icon-button-l
             {:icon ic/cancel
              :on-click #(rf/dispatch [::event/cancel-pyrometer-edit])}]]]
          (->> [[:name :serial-number]
                [:wavelength :date-of-calibration :tube-emissivity]]
               (map (fn [ks]
                      (->> ks
                           (map #(vector prop-field
                                         (assoc (props %) :path [%]))))))
               (interleave (repeat '([:br])))
               (drop 1)
               (apply concat)))))

(defn edit-pyrometer []
  (let [{:keys [height]} @(rf/subscribe [::ht-subs/view-size])
        index @(rf/subscribe [::subs/index])
        edit? (some? index)
        h (* (if edit? 0.3 0.4) height)
        pms @(rf/subscribe [::subs/data])]
    [ui/dialog {:open @(rf/subscribe [::subs/open?])
                :title (translate [:pyrometer :manage :title] "Manage IR pyrometers")}
     [:div (use-style style/body)
      (if (not-empty pms) [scroll-box {:style {:height h}}
                           (into [ui/menu {:value index}]
                                 (map item pms (range)))])
      (if edit? [edit])
      [:div (use-sub-style style/body :btns)
       [app-comp/button {:icon ic/plus
                         :disabled? edit?
                         :label (translate [:action :add :label] "Add")
                         :on-click #(rf/dispatch [::event/new-pyrometer])}]
       [app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
                         :icon ic/accept
                         :label (translate [:action :done :label] "Done")
                         :on-click #(rf/dispatch [::event/submit])}]
       [app-comp/button {:icon ic/cancel
                         :label (translate [:action :cancel :label] "Cancel")
                         :on-click #(rf/dispatch [::event/close])}]]]]))
