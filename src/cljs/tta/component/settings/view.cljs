;; view elements component setting
(ns tta.component.settings.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.common :as au]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.view :as app-view]
            [tta.app.scroll :refer [scroll-box]]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.root.event :as root-event]
            [tta.component.settings.style :as style]
            [tta.component.settings.subs :as subs]
            [tta.component.settings.event :as event]
            [tta.dialog.edit-pyrometer.view :refer [edit-pyrometer]]
            [tta.component.reformer-dwg.view :refer [reformer-dwg]]
            [tta.dialog.tube-prefs.view :refer [tube-prefs]]
            [tta.dialog.custom-emissivity.view :refer [custom-emissivity]]))

(defn form-cell [style error label widget]
  [:div (use-sub-style style :form-cell)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error)
    (if (fn? error) (error) error)]])

(defn form-cell-2 [style error label widget]
  [:div (use-sub-style style :form-cell-2)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error)
    (if (fn? error) (error) error)]])

(defn show-error? [] @(rf/subscribe [::subs/show-error?]))

(defn validity [{:keys [error valid?]}]
  (if (show-error?) [error valid?] [nil true]))

(defn temp-unit [style]
  (let [{:keys [value] :as field} @(rf/subscribe [::subs/field [:temp-unit]])
        [error valid?] (validity field)]
    [form-cell-2 style error
     (translate [:settings :temp-unit :label] "Temperature unit")
     [app-comp/dropdown-selector
      {:valid? valid?
       :on-select #(rf/dispatch [::event/set-temp-unit %])
       :selected value, :items [au/deg-C au/deg-F]}]]))

(defn target-temp [style]
  (let [{:keys [value] :as field} @(rf/subscribe [::subs/field-temp [:target-temp]])
        [error valid?] (validity field)]
    [form-cell-2 style error
     (translate [:settings :target-temp :label] "Target temperature")
     [app-comp/text-input
      {:on-change #(rf/dispatch [::event/set-temp [:target-temp] %])
       :value value, :valid? valid?}]]))

(defn design-temp [style]
  (let [{:keys [value] :as field} @(rf/subscribe [::subs/field-temp [:design-temp]])
        [error valid?] (validity field)]
    [form-cell-2 style error
     (translate [:settings :design-temp :label] "Design temperature")
     [app-comp/text-input
      {:on-change #(rf/dispatch [::event/set-temp [:design-temp] % true])
       :value value, :valid? valid?}]]))

(defn default-pyrometer [style]
  (let [{:keys [value] :as field} @(rf/subscribe [::subs/field [:pyrometer-id]])
        [error valid?] (validity field)
        pyrometers @(rf/subscribe [::subs/pyrometers])
        selected (some #(if (= (:id %) value) %) pyrometers)]
    [form-cell-2 style error
     (translate [:settings :default-pyrometer :label] "Default IR pyrometer")
     (list
      [app-comp/dropdown-selector
       {:key :selector
        :disabled? (empty? pyrometers)
        :valid? valid?
        :width (- (get-in style [::stylefy/sub-styles :data :c-w]) 100)
        :on-select #(rf/dispatch [::event/set-field [:pyrometer-id] (:id %) true])
        :selected selected, :items pyrometers
        :value-fn :id, :label-fn :name}]
      [app-comp/button {:key :edit
                        :icon ic/pyrometer+
                        :label (translate [:action :edit :label] "Edit")
                        :on-click #(rf/dispatch [:tta.dialog.edit-pyrometer.event/open])}])]))

(defn tube-emissivity [style]
  (let [{:keys [value] :as field} @(rf/subscribe [::subs/field [:emissivity-type]])
        [error valid?] (validity field)
        options @(rf/subscribe [::subs/emissivity-types])
        selected (some #(if (= (:id %) value) %) options)]
    [form-cell-2 style error
     (translate [:settings :tube-emissivity :label] "Tube emissivity")
     (list
      [app-comp/dropdown-selector
       {:key :selector
        :valid? valid?
        :width (- (get-in style [::stylefy/sub-styles :data :c-w]) 100)
        :on-select #(rf/dispatch [::event/set-emissivity-type 
                                  [:emissivity-type] 
                                  (:id %) true])
        :selected selected, :items options
        :value-fn :id, :label-fn :name, :disabled?-fn :disabled?}]
      (if (= value "custom")
        [app-comp/button {:key :edit
                          :icon ic/emissivity+
                          :label (translate [:action :edit :label] "Edit")
                          :on-click #(rf/dispatch [:tta.dialog.custom-emissivity.event/open])}]))]))

(defn min-tubes% [style]
  (let [{:keys [value] :as field} @(rf/subscribe [::subs/field [:min-tubes%]])
        [error valid?] (validity field)]
    [form-cell-2 style error
     (translate [:settings :min-tubes% :label] "Minimum % of tubes to measure")
     [app-comp/text-input
      {:on-change #(rf/dispatch [::event/set-number [:min-tubes%] %
                                 true {:max 100, :min 1}])
       :value value, :valid? valid?}]]))

(defn tube-pref [style]
  [form-cell-2 style nil
   (translate [:settings :tube-preference :label] "Tube preference")
   [app-comp/button {:icon ic/mark-tube
                     :label (translate [:action :edit :label] "Edit")
                     :on-click #(rf/dispatch [:tta.dialog.tube-prefs.event/open])}]]) 

(defn form [style]
  [:div (use-sub-style style :form)
   [temp-unit style]
   [target-temp style]
   [design-temp style]
   [default-pyrometer style]
   [tube-emissivity style]
   [min-tubes% style]
   [tube-pref style]])

(defn body [{:keys [width height]}]
  (let [w (* (- width 85) 0.6)
        h (- height 40)
        style (style/body width height)]
    [:div (use-style style)
     [reformer-dwg {:width w :height h}]
     [app-view/vertical-line {:height h}]
     [scroll-box (use-sub-style style :form-scroll)
      [form style]]
     ;;dialogs
     (if @(rf/subscribe [:tta.dialog.edit-pyrometer.subs/open?])
       [edit-pyrometer])
     (if @(rf/subscribe [:tta.dialog.tube-prefs.subs/open?])
       [tube-prefs])
     (if @(rf/subscribe [:tta.dialog.custom-emissivity.subs/open?])
       [custom-emissivity])]))

(defn no-config [{:keys [width height]}]
  [:div {:style {:width width, :height height}}
   [:div (use-style style/no-config)
    "Missing configuration!"]])

(defn settings []
  (let [config? @(rf/subscribe [::app-subs/config?])]
    [app-view/layout-main
     (translate [:settings :title :text] "Settings")
     (translate [:settings :title :sub-text] "Reformer preferences")
     [(if config?
        [app-comp/button {:disabled? (if (show-error?)
                                       (not @(rf/subscribe [::subs/can-submit?]))
                                       (not @(rf/subscribe [::subs/dirty?])))
                          :icon ic/upload
                          :label (translate [:action :upload :label] "Upload")
                          :on-click #(rf/dispatch [::event/upload])}])
      [app-comp/button {:icon ic/cancel
                        :label (translate [:action :cancel :label] "Cancel")
                        :on-click #(rf/dispatch [::root-event/activate-content :home])}]]
     (if config?
       body
       no-config)]))
