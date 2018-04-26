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
            [tta.app.input :refer [list-tube-view-factor]]
            [tta.app.view :as app-view]
            [tta.dialog.view-factor.subs :as subs]
            [tta.dialog.view-factor.style :as style]
            [tta.dialog.view-factor.event :as event]
            [re-frame.events :as events]))


(def container (r/atom {}))

(defn get-view-key [level-key index]
  (case level-key
    :top
    (case index
      0 :ceiling 1 :wall 2 :wall 3 :ceiling)
    :middle :wall
    :bottom (case index
              0 :floor 1 :wall 2 :wall 3 :floor)))

(defn get-side-index [level-key index]
  (case level-key
    :top
    (case index
      0 0
      1 0
      2 1
      3 1)
    :bottom (case index
              0 0  1 0 2 1 3 1)
    :middle (case index 0 0 1 1))) 

(defn fill-all-selection []
  (let [config @(rf/subscribe [::subs/config])
        opts @(rf/subscribe [::subs/row-options])
        val @(rf/subscribe [::subs/row-selection])
        selected (some #(if (= (:id %) val) %) opts)]
    [:span (use-style style/form-field)
     [app-comp/dropdown-selector
      {:width 70
       :selected selected
       :items opts
       :label-fn :label , :value-fn :id
       :on-select #(rf/dispatch [::event/set-row-selection (:id %)])}]]))

(defn fill-wall [style]
  (let [{:keys [value error valid?]}
        @(rf/subscribe [::subs/field [:fill-all-wall]])]
    [:span (use-style style/form-field)
     [:span (use-sub-style style/form-field :label)
      (translate [:config :wall :label] "Wall")]
     [:span (use-sub-style style/form-field :error)
      (if (fn? error)
        (error)
        error)]
     [app-comp/text-input
      {:valid?  valid?
       :width 60
       :value value
       :on-change #(rf/dispatch
                    [::event/set-wall-fill-all-field %])}]
     [app-comp/button
      {:disabled?  (or (not valid?)
                       (empty? value))
       :icon ic/dataset
       :width 100
       :label (translate [:action :fill-all :label] "Fill Wall")
       :on-click #(rf/dispatch
                   [::event/fill-all :wall value])}]]))

(defn fill-ceiling [style]
  (let [{:keys [value error valid?]}
        @(rf/subscribe [::subs/field [:fill-all-ceiling]])]
    [:span (use-style style/form-field)
     [:span (use-sub-style style/form-field :label)
      (translate [:config :ceiling :label] "Ceiling")]
     [:span (use-sub-style style/form-field :error)
      (if (fn? error)
        (error)
        error)]
     [app-comp/text-input
      {:valid?  valid?
       :width 60
       :value value
       :on-change #(rf/dispatch
                    [::event/set-ceiling-fill-all-field %])}]
     [app-comp/button
      {:disabled?  (or (not valid?)
                       (empty? value))
       :icon ic/dataset
       :width 100
       :label (translate [:action :fill-all :label] "Fill Ceiling")
       :on-click #(rf/dispatch
                   [::event/fill-all :ceiling value])}]]))

(defn fill-floor [style]
  (let [{:keys [value error valid?]}
        @(rf/subscribe [::subs/field [:fill-all-floor]])]
    [:span (use-style style/form-field)
     [:span (use-sub-style style/form-field :label)
      (translate [:config :floor :label] "Floor")]
     [:span (use-sub-style style/form-field :error)
      (if (fn? error)
        (error)
        error)]
     [app-comp/text-input
      {:valid?  valid?
       :width 60
       :value value
       :on-change #(rf/dispatch
                    [::event/set-floor-fill-all-field %])}]
     [app-comp/button
      {:disabled?  (or (not valid?)
                       (empty? value))
       :icon ic/dataset
       :width 100
       :label (translate [:action :fill-all :label] "Fill Floor")
       :on-click #(rf/dispatch
                   [::event/fill-all :floor value])}]]))

(defn fill-all-component []
  (let [level-key @(rf/subscribe [::subs/selected-level-key])] 
    [:div {:style {:width (:width @container)
                   :height 55}}
     (case level-key
       :top (fill-ceiling nil)
       :bottom (fill-floor nil)
       nil)
     (fill-wall nil)
     (fill-all-selection)]))


(defn content-render
  [{:keys [height width], [_ sel] :selected}]
  (let [{:keys [tube-rows tube-row-count]}
        (:tf-config @(rf/subscribe [::subs/config]))
        sel-key @(rf/subscribe [::subs/level-key (or sel 0)])
        num-field (if (or (= sel-key :bottom) (= sel-key :top)) 4 2) 
        render-fn (fn [indexes show]
                    (map (fn [i]
                           (let [{:keys [name tube-count start-tube
                                         end-tube]}
                                 (get tube-rows i)] 
                             [list-tube-view-factor
                              {:label name
                               :height (- height 40)
                               :start-tube start-tube
                               :end-tube end-tube
                               :field-fn 
                               (fn [in s] @(rf/subscribe
                                           [::subs/view-factor-field
                                            sel-key
                                            (get-view-key sel-key s)
                                            i
                                            (get-side-index sel-key s)
                                            in])) 
                               :level sel-key
                               :pref-fn #(deref (rf/subscribe [::subs/tube-pref i %1]))
                               :num-field num-field
                               :on-clear #(rf/dispatch [::event/clear-row i])
                               :on-change
                               (fn [in s v] (rf/dispatch
                                            [::event/set-view-factor-field
                                             sel-key
                                             (get-view-key sel-key s)
                                             i
                                             (get-side-index sel-key s)
                                             in v]))}])) indexes))]
    [lazy-cols {:height height
                :width width
                :item-width (+ 40 (* num-field 110)) 
                :item-count tube-row-count
                :items-render-fn render-fn}]))

(defn view-factor-component [config data h]
  (r/create-class
   {:component-did-mount
    (fn [this]
      (swap! container  assoc
             :width (i/oget-in this [:refs :container :offsetWidth])))
    :reagent-render
    (fn [config data]
      (let [level-opts @(rf/subscribe [::subs/level-opts])
            label-level (map #(get % :label) level-opts)]
        [:div {:ref "container"}
         [app-view/tab-layout
          {:bottom-tabs
           {:labels label-level
            :selected @(rf/subscribe
                        [::subs/selected-level])
            :on-select #(rf/dispatch [::event/set-level %])}
           :width (:width @container)
           :height h
           :content content-render}]]))}))

(defn view-factor []
  (let [{:keys [height]} @(rf/subscribe [::ht-subs/view-size])
        h (* 0.5 height)
        open? @(rf/subscribe [::subs/open?])
        config @(rf/subscribe [::subs/config])
        data @(rf/subscribe [::subs/data])]
    [ui/dialog
     {:open open?
      :title (translate [:view-factor :manage :title]
                        "View Factor")
      :actions (r/as-element
                [:div
                 [app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
                                   :icon ic/accept
                                   :label (translate [:action :done :label] "Done")
                                   :on-click #(rf/dispatch [::event/submit])}]
                 [app-comp/button {:icon ic/cancel
                                   :label (translate [:action :cancel :label] "Cancel")
                                   :on-click #(rf/dispatch [::event/close])}]])}
     [:div
      [fill-all-component h]
      [view-factor-component config data h]]]))
