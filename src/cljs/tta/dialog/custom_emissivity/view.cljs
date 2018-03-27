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

(defn render-tubes [plant height]
  (let [items-render-fn
        (fn [indexes show-item]
          (map (fn [i]
                 (let [
                       {:keys [start-tube end-tube tube-count]}
                       (or
                         (get-in plant [:config :sf-config :chambers i])
                         (get-in plant [:config :tf-config :tube-rows i]))
                       level @(rf/subscribe [::subs/selected-level-index])
                       firing (get-in plant [:config :firing])
                       label (case firing
                               "side" "Chamber"
                               "top" "Row")]
                   [input/list-tube-both-sides
                    {:label      (str label " " (inc i))
                     :height     (- height 140)
                     :start-tube start-tube
                     :end-tube   end-tube
                     :field-fn   (fn [%1 %2]
                                   (case firing
                                     "side" @(rf/subscribe [::subs/field (conj [] i %2 %1)])
                                     "top" @(rf/subscribe [::subs/field (conj [] level i %2 %1)])))
                     :pref-fn    (fn [%1 %2]
                                   @(rf/subscribe [::subs/tube-pref %1 %2]))
                     :on-change  (case firing
                                   "side" #(rf/dispatch
                                             [::event/set-field
                                              (conj [] i %2 %1) %3 false
                                              {:max 0.99 :min 0.01 :precision 2}])
                                   "top" #(rf/dispatch
                                            [::event/set-field
                                             (conj [] level i %2 %1) %3 false
                                             {:max 0.99 :min 0.01 :precision 2}]))
                     :on-clear   #(rf/dispatch
                                    [::event/clear-custom-emissivity i])}]))
               indexes))]
    [lazy-cols {:height          (- height 120)
                :width           (- (:width @container) 30)
                :item-width      220
                :item-count      (count (or
                                          (get-in plant [:config :sf-config :chambers])
                                          (get-in plant [:config :tf-config :tube-rows])))
                :items-render-fn items-render-fn}]))

(defn fill-all []
  (let [{:keys [value valid? error]} @(rf/subscribe [::subs/field [:fill-all]])
        error (if (fn? error) (error) error)]
    [:div (use-style style/form-field)
     #_[:span (use-sub-style style/form-field :label)
        (translate [:custom-emissivity :fill-all :label] "Fill all")]
     [app-comp/text-input
      {:on-change #(rf/dispatch
                     [::event/set-fill-all-field
                      [:fill-all] % false
                      {:max 0.99 :min 0.01 :precision 2}])
       :value value, :valid? valid?}]
     [:span (use-sub-style style/form-field :error) error]
     [app-comp/button
      {:disabled? (not valid?)
       :icon      ic/dataset
       :label     (translate [:action :fill-all :label] "Fill all")
       :on-click  #(rf/dispatch [::event/fill-all])}]]))

(defn custom-emissivity-component [height]
  (r/create-class
    {:component-did-mount
     (fn [this]
       (swap! container assoc
              :width (i/oget-in this [:refs :container :offsetWidth])
              :height height))
     :reagent-render
     (fn [height]
       [:div {:ref "container"}
        (let [tab-opts @(rf/subscribe [::subs/tab-opts])
              plant @(rf/subscribe [::app-subs/plant])]

          [app-view/tab-layout
           {:bottom-tabs
            (case (get-in plant [:config :firing])
              "side" nil
              "top" {:labels    tab-opts
                     :selected  @(rf/subscribe [::subs/selected-level-index])
                     :on-select #(rf/dispatch [::event/set-level-index %])}
              "default")
            :width  (:width @container)
            :height height
            :content (fn []
                       [:div
                        (fill-all)
                        (render-tubes plant height)])}])])}))

(defn custom-emissivity []
  (let [{:keys [height]} @(rf/subscribe [::ht-subs/view-size])
        h (* 0.5 height)]
    [ui/dialog {:open  @(rf/subscribe [::subs/open?])
                :title (translate [:custom-emissivity :manage :title] "Manage Custom Emissivity")}
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
