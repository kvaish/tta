;; view elements dialog custom-emissivity
(ns tta.dialog.custom-emissivity.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.comp :as ht-comp]
            [ht.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [ht.util.interop :as i]
            [tta.app.icon :as ic]
            [tta.app.input :as input]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.comp :as app-comp]
            [tta.app.style :as app-style]
            [tta.app.view :as app-view]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.dialog.custom-emissivity.style :as style]
            [tta.dialog.custom-emissivity.subs :as subs]
            [tta.dialog.custom-emissivity.event :as event]
            [tta.component.settings.subs :as setting-subs]))

(def container (r/atom {}))

(defn tube-col [sel-level-key height col-index {:keys [start-tube end-tube name]}]
  (let [data       @(rf/subscribe [::subs/data])
        has-data?  (some #(some some? %)
                         (get-in data
                                 [:levels sel-level-key :tube-rows col-index
                                  :custom-emissivity]))
        field-path [:levels sel-level-key :tube-rows col-index :custom-emissivity]]
    [input/list-tube-both-sides
     {:label      name
      :height     height
      :start-tube start-tube
      :end-tube   end-tube
      :field-fn   (fn [tube side]
                    @(rf/subscribe [::subs/field (conj field-path side tube)]))
      :pref-fn    (fn [tube]
                    @(rf/subscribe [::subs/tube-pref col-index tube]))
      :on-change  (fn [tube side value]
                    (rf/dispatch [::event/set-emissivity-field
                                  (conj field-path side tube) value]))
      :on-clear   (if has-data? #(rf/dispatch
                                  [::event/clear-custom-emissivity col-index]))}]))



(defn render-tubes [height sel-level-key]
  (let [plant @(rf/subscribe [::app-subs/plant])
        tube-configs (or
                      (get-in plant [:config :sf-config :chambers])
                      (get-in plant [:config :tf-config :tube-rows]))
        data @(rf/subscribe [::subs/data])
        row-count (count (get-in data [:levels sel-level-key :tube-rows]))
        items-render-fn
        (fn [indexes show-item]
          (map (fn [i]
                 [tube-col sel-level-key (- height 20) i (get tube-configs i)])
               indexes))]
    [lazy-cols {:height          height
                :width           (- (:width @container) 30)
                :item-width      220
                :item-count       row-count
                :items-render-fn items-render-fn}]))

(defn fill-all []
  (let [{:keys [value valid? error]} @(rf/subscribe [::subs/field [:fill-all]])
        error (if (fn? error) (error) error)]
    [:div (use-style style/form-field)
     [app-comp/text-input
      {:on-change #(rf/dispatch [::event/set-fill-all-field %])
       :value value, :valid? valid?}]
     [:span (use-sub-style style/form-field :error) error]
     [app-comp/button
      {:disabled? (or (not valid?)
                      (empty? value))
       :icon      ic/dataset
       :label     (translate [:action :fill-all :label] "Fill all")
       :on-click  #(rf/dispatch [::event/fill-all])}]]))

(defn content-render [{:keys [height], [_ sel] :selected}]
  (let [level-key @(rf/subscribe [::subs/level-key sel])]
    [:div
     [fill-all]
     [render-tubes (- height 60) level-key]]))

(defn custom-emissivity-component [height]
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
          {:bottom-tabs {:labels    level-labels
                         :selected  @(rf/subscribe [::subs/selected-level])
                         :on-select #(rf/dispatch [::event/set-level %])}
           :width  (:width @container)
           :height height
           :content content-render}])])}))

(defn custom-emissivity []
  (let [{:keys [height]} @(rf/subscribe [::ht-subs/view-size])
        h (* 0.5 height)]
    [ui/dialog {:open  @(rf/subscribe [::subs/open?])
                :title (translate [:custom-emissivity :manage :title]
                                  "Manage custom emissivity")}
     [:div (use-style style/body)
      [custom-emissivity-component h]
      [:div (use-sub-style style/body :btns)
       [app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
                         :icon ic/accept
                         :label (translate [:action :add :label] "Accept")
                         :on-click #(rf/dispatch [::event/submit])}]
       [app-comp/button {:icon     ic/cancel
                         :label    (translate [:action :cancel :label] "Cancel")
                         :on-click #(rf/dispatch [::event/close])}]]]]))
