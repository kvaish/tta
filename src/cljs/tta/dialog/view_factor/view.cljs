;; view elements dialog view-factor
(ns tta.dialog.view-factor.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.util.interop :as i]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.input :refer [list-tube-view-factors]]
            [tta.app.view :as app-view]
            [tta.dialog.view-factor.subs :as subs]
            [tta.dialog.view-factor.style :as style]
            [tta.dialog.view-factor.event :as event]
            [re-frame.events :as events]))

(defn fill-all-row-selection []
  (let [opts @(rf/subscribe [::subs/row-options])
        val @(rf/subscribe [::subs/row-selection])
        selected (some #(if (= (:id %) val) %) opts)]
    [:span (use-style style/form-field)
     [:span (use-sub-style style/form-field :label)
      (translate [:view-factor :fill-all :rows] "Fill rows:")]
     [app-comp/dropdown-selector
      {:width 70
       :selected selected
       :items opts
       :label-fn :label , :value-fn :id
       :on-select #(rf/dispatch [::event/set-row-selection (:id %)])}]]))

(defn fill-all-wall [level-key wall-type]
  (let [{:keys [value error valid?]} @(rf/subscribe
                                       [::subs/field [:fill-all wall-type]])
        error (if (fn? error) (error) error)]
    [:span (use-style style/form-field)
     [app-comp/text-input
      {:value value, :valid? valid?
       :width 60
       :on-change #(rf/dispatch [::event/set-fill-all wall-type %])}]
     [:span (use-sub-style style/form-field :error) error]
     [app-comp/button
      {:disabled? (or (not valid?) (empty? value))
       :icon ic/dataset
       :width 100
       :label (case wall-type
                :ceiling (translate [:action :fill-all :ceiling] "Fill Ceiling")
                :floor (translate [:action :fill-all :floor] "Fill Floor")
                (translate [:action :fill-all :wall] "Fill Wall"))
       :on-click #(rf/dispatch [::event/fill-all level-key wall-type value])}]]))

(defn fill-all [width height level-key]
  [:div {:style {:width width, :height height}}
   (fill-all-row-selection)
   (fill-all-wall level-key :wall)
   (case level-key
     :top (fill-all-wall level-key :ceiling)
     :bottom (fill-all-wall level-key :floor)
     nil)])

(defn tube-row-comp [_ level-key row-index row-config]
  (let [{:keys [name start-tube end-tube tube-count]} row-config
        pref-fn (fn [tube-index]
                  @(rf/subscribe [::subs/tube-pref row-index tube-index]))
        on-clear #(rf/dispatch [::event/clear-tube-row level-key row-index tube-count])
        field-fn (fn [tube-index side-index wall-type]
                   @(rf/subscribe [::subs/view-factor-field level-key row-index
                                   wall-type side-index tube-index]))
        on-change (fn [tube-index side-index wall-type value]
                    (rf/dispatch [::event/set-view-factor-field level-key row-index
                                  wall-type side-index tube-index value]))]
    (fn [height _ _ _]
      (let [has-data? @(rf/subscribe [::subs/has-data? level-key row-index])]
        [list-tube-view-factors {:level-key level-key
                                 :height (- height 20)
                                 :label name
                                 :start-tube start-tube
                                 :end-tube end-tube
                                 :field-fn field-fn
                                 :pref-fn pref-fn
                                 :on-clear (if has-data? on-clear)
                                 :on-change on-change}]))))

(defn render-tubes [width height level-key]
  (let [{:keys [tube-rows tube-row-count]} (:tf-config @(rf/subscribe [::subs/config]))
        field-count (if (= :middle level-key) 2 4)
        items-render-fn
        (fn [indexes show-item]
          (map (fn [i]
                 [tube-row-comp (- height 20) level-key i (get tube-rows i)])
               indexes))]
    [lazy-cols {:width width, :height height
                :item-width (+ 10 24 46 (* field-count 86))
                :item-count tube-row-count
                :items-render-fn items-render-fn}]))

(defn content-comp [{:keys [width height], [_ sel] :selected}]
  (let [level-key @(rf/subscribe [::subs/level-key sel])]
    [:div
     [fill-all width 60 level-key]
     [render-tubes width (- height 60) level-key]]))

(defn view-factor-body [height]
  (let [container (r/atom {})]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! container assoc
               :width (i/oget-in this [:refs :container :offsetWidth])))
      :reagent-render
      (fn [height]
        [:div {:ref "container"}
         (let [level-opts @(rf/subscribe [::subs/level-opts])
               level-labels (map #(get % :label) level-opts)]
           [app-view/tab-layout
            {:bottom-tabs {:labels level-labels
                           :selected @(rf/subscribe [::subs/selected-level])
                           :on-select #(rf/dispatch [::event/set-selected-level %])}
             :width (:width @container)
             :height height
             :content content-comp}])])})))

(defn view-factor []
  (let [{:keys [height]} @(rf/subscribe [::ht-subs/view-size])]
    [ui/dialog
     {:open @(rf/subscribe [::subs/open?])
      :title (translate [:view-factor :manage :title] "View Factor")}
     [:div (use-style style/body)
      [view-factor-body (* 0.5 height)]
      [:div (use-sub-style style/body :btns)
       [app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
                         :icon ic/accept
                         :label (translate [:action :done :label] "Done")
                         :on-click #(rf/dispatch [::event/submit])}]
       [app-comp/button {:icon ic/cancel
                         :label (translate [:action :cancel :label] "Cancel")
                         :on-click #(rf/dispatch [::event/close])}]]]]))
