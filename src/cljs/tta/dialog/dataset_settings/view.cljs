;; view elements dialog dataset-settings
(ns tta.dialog.dataset-settings.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.comp :as ht-comp]
            [ht.util.common :refer [to-date-time-map from-date-time-map]]
            [ht.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.comp :as app-comp]
            [tta.app.calendar :refer [date-picker]]
            [tta.app.icon :as ic]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.dialog.dataset-settings.style :as style]
            [tta.dialog.dataset-settings.subs :as subs]
            [tta.dialog.dataset-settings.event :as event]))

(defn show-error? [] true #_@(rf/subscribe [::subs/show-error?]))

(defn query-id [query-id & params]
  (let [{:keys [value error valid?]} @(rf/subscribe (into [query-id] params))
        show-err? (show-error?)]
    {:value value
     :error (if show-err? (if (fn? error) (error) error))
     :valid? (if show-err? valid? true)}))

(defn form-cell
  ([style skey label widgets]
   [:div (use-sub-style style skey)
    [:span (use-sub-style style :form-label) label]
    (into [:div] widgets)])
  ([style skey error label widget]
   [:div (use-sub-style style skey)
    [:span (use-sub-style style :form-label) label]
    widget
    [:span (use-sub-style style :form-error) error]]))

(defn form-cell-1
  ([style label widgets] (form-cell style :form-cell-1 label widgets))
  ([style error label widget] (form-cell style :form-cell-1 error label widget)))

(defn form-cell-2
  ([style label widgets] (form-cell style :form-cell-2 label widgets))
  ([style error label widget] (form-cell style :form-cell-2 error label widget)))

(defn form-cell-3
  ([style label widgets] (form-cell style :form-cell-3 label widgets))
  ([style error label widget] (form-cell style :form-cell-3 error label widget)))


(defn form-cell-4x3
  ([style label widgets] (form-cell style :form-cell-4x3 label widgets))
  ([style error label widget] (form-cell style :form-cell-4x3 error label widget)))

(defn date-time-of-reading [style]
  (let [date (update
              (to-date-time-map (:value @(rf/subscribe [::subs/field [:data-date]])))
              :minute #(- % (mod % 5)))
        hour-opts @(rf/subscribe [::subs/hour-opts])
        min-opts @(rf/subscribe [::subs/min-opts])
        hr-selected (some #(if (= (:id %) (:hour date)) %) hour-opts)
        min-selected  (some
                       #(if (= (:id %) (:minute date))%)
                       min-opts)]
    [:div (use-sub-style style :form-cell-1)
     [:div (use-sub-style style :form-label)
      (translate [:dataset-setting :reading-time :label] "Date of reading")]
     [date-picker {:date date
                   :valid? true
                   :max (to-date-time-map (js/Date.))
                   :on-change #(rf/dispatch [::event/set-field [:data-date]
                                             (from-date-time-map
                                              (assoc date :day (:day %)
                                                     :month (:month %)
                                                     :year (:year %)))])}]
     [:div (use-sub-style style :form-label)
      (translate [:dataset-setting :reading-time :label] "Time (HH:MM)")]
     [app-comp/dropdown-selector
      {:item-width 50
       :selected hr-selected
       :value-fn :id
       :label-fn :name
       :on-select #(rf/dispatch [::event/set-field [:data-date]
                                 (from-date-time-map (assoc date :hour (:id %)))])
       :items hour-opts}]
     [app-comp/dropdown-selector
      {:width 10
       :selected min-selected
       :value-fn :id
       :label-fn :name
       :on-select #(rf/dispatch [::event/set-field [:data-date]
                                 (from-date-time-map (assoc date :minute (:id %)))])
       :items min-opts}]]))


(defn internal-dataset [style]
  (let [topsoe?  (:value @(rf/subscribe [::subs/field [:topsoe?]]))]
    [form-cell-2 style nil
     (translate [:dataset-settings :topsoe :label]
                "HT Internal data set")
     [app-comp/toggle
      {:value topsoe?
       :on-toggle #(rf/dispatch [::event/set-field [:topsoe?] %])}]]))

(defn select-pyrometer [style]
  (let [pyrometers @(rf/subscribe [::subs/pyrometers])
        active-pyrometer (:value @(rf/subscribe [::subs/field [:pyrometer]]))
        selected-pyro (some #(if (= (:id active-pyrometer) (:id %))
                               %) pyrometers)]
    [form-cell-2 style nil
     (translate [:dataset-settings :pyrometer :label] "Select IR Pyrometer")
     [app-comp/dropdown-selector
      {:width 100
       :selected selected-pyro
       :value-fn :id  :label-fn :name
       :items pyrometers
       :on-select #(rf/dispatch [::event/set-pyrometer (:id %)])}]]))

(defn emissivity-type [style]
  (let [{:keys [value error valid?]} (query-id ::subs/field [:emissivity-type])
        emissivity-opts @(rf/subscribe [::subs/emissivity-types])
        selected (some #(if (= value (:id %)) %) emissivity-opts)]
    [form-cell-2 style error
     (translate [:dataset-settings :emissivity-type :label]
                "Tube emissivity setting")
     [app-comp/dropdown-selector
      {:selected selected
       :width 120
       :items emissivity-opts
       :label-fn :label, :value-fn :id
       :on-select #(rf/dispatch [::event/set-emissivity-type (:id %)])}]]))

(defn tube-emissivity [style]
  (let [pyro-emissivity  (:tube-emissivity
                          (:value @(rf/subscribe [::subs/field [:pyrometer]])))
        emissivity (:value @(rf/subscribe [::subs/field [:emissivity]]))
        {:keys [value error valid?]} (query-id ::subs/field [:emissivity])]

    [:span (use-sub-style style :form-cell-2)
     [:span (merge (use-sub-style style :form-label)
                   #_(if emissivity {:style {:text-decoration "line-through"}}))
      [:div (use-sub-style style :form-error) error]
      (str (translate [:dataset-settings :pyrometer-emissivity :label]
                      "Tube emissivity")
           " : "
           pyro-emissivity
           " "
           (translate [:dataset-settings :override-emissivity :label]
                      "Override emissivity"))]
     [app-comp/text-input
      {:width 50 
       :value value
       :valid? valid? 
       :on-change #(rf/dispatch [::event/set-override-emissivity %])}]]))

(defn emissivity-setting [style]
  (let [{:keys [value error valid?]} (query-id ::subs/field [:emissivity-setting])]
    [form-cell-2 style error
     (translate [:dataset-settings :emissivity-setting :label]
                "Emissivity setting on IR Pyrometer") 
     [app-comp/text-input
      {:width 75
       :value value
       :valid? valid? 
       :on-change #(rf/dispatch [::event/set-emissivity-setting %])}]]))

(defn pyrometer-emissivity-warning [style]
  (let [{:keys [value]} (query-id ::subs/field [:emissivity-setting])]
    (if-not (or (= 1 value) (= "1" value))
      [:div (use-sub-style style :form-cell-1)
       [:div (merge (use-sub-style style :form-label)
                    {:style {:color (ht-style/color :red)}})
        (translate [:dataset-settings :emissivity-warning :message]
                   "Please check the latest calibration report. 
If nothing is stated about a specific emissivity (
setting you should always use 1.0")]
       ])))

(defn roles [style]
  (let [role-opts @(rf/subscribe [:ht.app.subs/user-roles])
        {:keys [value valid? error]} (query-id ::subs/field [:role-type])
        selected (some #(if (= (:id value) (:id %)) %) role-opts)]
    [form-cell-3 style error
     (translate [:dataset-settings :role-type :label] "Role")
     [app-comp/dropdown-selector
      {:items role-opts
       :width 100
       :valid? valid?
       :selected selected
       :label-fn :name :value-fn :id
       :on-select #(rf/dispatch [::event/set-field [:role-type] % true])}]]))

(defn operator [style]
  (let [{:keys [value valid? error]} (query-id ::subs/field [:operator])]
    [form-cell-3 style error
     (translate [:dataset-settings :operator :label] "Operator")
     [app-comp/text-input
      {:width 100
       :value value
       :valid? valid?
       :on-change #(rf/dispatch [::event/set-field [:operator] % false])}]]))


(defn shift [style]
  (let [{:keys [value valid? error]} (query-id ::subs/field [:shift])]
    [form-cell-3 style error
     (translate [:dataset-settings :shift :label] "Shift")
     [app-comp/text-input
      {:width 105
       :value value
       :valid? valid?
       :on-change #(rf/dispatch [::event/set-field [:shift] % false])}]]))

(defn comments [style]
  (let [{:keys [value valid? error]} (query-id ::subs/field [:comments])]
    [form-cell-1 style error
     (translate [:dataset-settings :comments :label] "Comments")
     [app-comp/text-area
      {:width 500 
       :text value
       :height 100
       :on-change #(rf/dispatch [::event/set-field [:comments] % false])}]]))

(defn dataset-settings []
  (let [open? @(rf/subscribe [::subs/open?])
        topsoe-user? @(rf/subscribe [:ht.app.subs/topsoe?])
        title (translate [:dataset-settings :dialog :title] "Dataset settings")
        style (style/body 1400 1400)]
    [ui/dialog
     {:open open? 
      :modal true
      :title title
      :actions (r/as-element
                [:div
                 [app-comp/button {:icon ic/cancel
                                   :label (translate [:action :cancel :label] "Cancel")
                                   :on-click #(rf/dispatch [::event/close])}]
                 [app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
                                   :icon ic/accept
                                   :label (translate [:action :done :label] "Done")
                                   :on-click #(rf/dispatch [::event/submit])}]])}
     
     [:div
      (date-time-of-reading style)]
     [:div (if topsoe-user?
             (internal-dataset style))]
     [:div
      (select-pyrometer style)
      (emissivity-setting style)]
     [:div (pyrometer-emissivity-warning style)]
     [:div (emissivity-type style)
      (tube-emissivity style)]
     [:div (use-sub-style style :form-heading-label)
      (translate [:dataset-settings :aditional-settings :label]
                 "Additional Settings")]
     [:div (roles style)
      (operator style)
      (shift style)]
     [:div (comments style)]]))
