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
            [tta.component.reformer-dwg.view :refer [reformer-dwg]]))

(defn form-cell [style error label widget]
  [:div (use-sub-style style :form-cell)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error) error]])

(defn form-cell-2 [style error label widget]
  [:div (use-sub-style style :form-cell-2)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error) error]])

(defn temp-unit [style]
  (let [{:keys [value error valid?]} @(rf/subscribe [::subs/field [:temp-unit]])]
    [form-cell-2 style error
     (translate [:settings :temp-unit :label] "Temperature unit")
     [app-comp/dropdown-selector
      {:valid? valid?
       :on-select #(rf/dispatch [::event/set-temp-unit %])
       :selected value, :items [au/deg-C au/deg-F]}]]))

(defn target-temp [style]
  (let [{:keys [value error valid?]} @(rf/subscribe [::subs/field-temp [:target-temp]])]
    [form-cell-2 style error
     (translate [:settings :target-temp :label] "Target temperature")
     [app-comp/text-input
      {:on-change #(rf/dispatch [::event/set-temp [:target-temp] %])
       :value value, :valid? valid?}]]))

(defn design-temp [style]
  (let [{:keys [value error valid?]} @(rf/subscribe [::subs/field-temp [:design-temp]])]
    [form-cell-2 style error
     (translate [:settings :design-temp :label] "Design temperature")
     [app-comp/text-input
      {:on-change #(rf/dispatch [::event/set-temp [:design-temp] % true])
       :value value, :valid? valid?}]]))

(defn default-pyrometer [style]
  (let [{:keys [value error valid?]} @(rf/subscribe [::subs/field [:pyrometer-id]])
        pyrometers @(rf/subscribe [::subs/pyrometers])
        selected (some #(if (= (:id %) value) %) pyrometers)]
    [form-cell-2 style error
     (translate [:settings :default-pyrometer :label] "Default IR pyrometer")
     [app-comp/dropdown-selector
      {:valid? valid?
       :width (- (get-in style [::stylefy/sub-styles :data :c-w]) 100)
       :on-select #(rf/dispatch [::event/set-field [:pyrometer-id] (:id %) true])
       :selected selected, :items pyrometers
       :value-fn :id, :label-fn :name
       :left-icon ic/pyrometer+
       :left-action #(js/console.log "manage pyrometers")}]]))

(defn tube-emissivity [style]
  (let [{:keys [value error valid?]} @(rf/subscribe [::subs/field [:emissivity-type]])
        options @(rf/subscribe [::subs/emissivity-types])
        selected (some #(if (= (:id %) value) %) options)]
    [form-cell-2 style error
     (translate [:settings :tube-emissivity :label] "Tube emissivity")
     [app-comp/dropdown-selector
      {:valid? valid?
       :width (- (get-in style [::stylefy/sub-styles :data :c-w]) 100)
       :on-select #(rf/dispatch [::event/set-field [:emissivity-type] (:id %) true])
       :selected selected, :items options
       :value-fn :id, :label-fn :name, :disabled?-fn :disabled?
       :left-icon ic/emissivity+
       :left-action #(js/console.log "custom emissivity")}]]))

(defn min-tubes% [style]
  (let [{:keys [value error valid?]} @(rf/subscribe [::subs/field [:min-tubes%]])]
    [form-cell-2 style error
     (translate [:settings :min-tubes% :label] "Minimum % of tubes to measure")
     [app-comp/text-input
      {:on-change #(rf/dispatch [::event/set-number [:min-tubes%] %
                                 true {:max 100, :min 20}])
       :value value, :valid? valid?}]]))

(defn tube-pref [style]
  [form-cell-2 style nil
   (translate [:settings :tube-preference :label] "Tube preference")
   [app-comp/button {:icon ic/mark-tube
                     :label (translate [:action :edit :label] "Edit")
                     :on-click #(js/console.log "todo: edit tube pref")}]]) ;;TODO:

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
      [form style]]]))

(defn settings []
  [app-view/layout-main
   (translate [:settings :title :text] "Settings")
   (translate [:settings :title :sub-text] "Reformer preferences")
   [[app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
                      :icon ic/upload
                      :label (translate [:action :upload :label] "Upload")
                      :on-click #(rf/dispatch [::event/upload])}]
    [app-comp/button {:icon ic/cancel
                      :label (translate [:action :cancel :label] "Cancel")
                      :on-click #(rf/dispatch [::root-event/activate-content :home])}]]
   body])
