;; view elements component config
(ns tta.component.config.view
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
            [tta.component.config.style :as style]
            [tta.component.root.event :as root-event]
            [tta.component.config.subs :as subs]
            [tta.component.config.event :as event]
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

(defn form-cell-3 [style error label widget]
  [:div (use-sub-style style :form-cell-3)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error) error]])

(defn form-cell-4 [style error label widget]
  [:div (use-sub-style style :form-cell-4)
   [:span (use-sub-style style :form-label) label]
   widget
   [:span (use-sub-style style :form-error) error]])

(defn text-input
  "[valid value event path validations]"
  [opts]
  (let [[valid? value event path validations] opts]
    [app-comp/text-input
     {:width     120
      ;:align     "center"
      :value     value
      :valid?    valid?
      :on-change #(rf/dispatch [event path % validations])}]))

(defn select-input
  "[valid options selected event path value-fn label-fn]"
  [opts]
  (let [[valid? options selected event path value-fn label-fn] opts]
    [app-comp/dropdown-selector
     {:valid?    valid?
      :items     options
      :selected  selected
      :on-select #(rf/dispatch [event path %])
      :value-fn  value-fn
      :label-fn  label-fn}]))

(defn toggle-input
  "[value event path]"
  [opts]
  (let [[value event path] opts]
    [app-comp/toggle
     {:value     value
      :on-toggle #(rf/dispatch [event path %])}]))

(defn rf-name [style]
  [form-cell-4 style nil
   (translate [:config :name :label] "Reformer name")
   (let [{:keys [:value :error :valid?]}
         @(rf/subscribe [::subs/field [:name]])]
     (text-input [valid? value ::event/set-field [:name]]))])

(defn dual-chamber [style]
  [form-cell style nil
   (translate [:config :dual-chamber? :label] "Dual chamber")
   (let [chs @(rf/subscribe [::subs/ch-count])
         val (case chs
               2 true
               1 false)]
     (toggle-input [val ::event/set-dual-chamber [:sf-config :dual-chamber?]]))])

(defn dual-nozzle [style]
  [form-cell style nil
   (translate [:config :dual-fuel-nozzle? :label] "Dual fuel nozzle")
   (let [val @(rf/subscribe [::subs/config [:dual-nozzle?]])]
     (toggle-input [val ::event/set-field [:sf-config :dual-nozzle?]]))])

(defn ch-common-field [style label key]
  (let [{:keys [:value :valid? :error]} @(rf/subscribe [::subs/ch-common-field [key]])]
    [form-cell-3 style error
     label
     (text-input [valid? value ::event/set-ch-common-field-sf [key]])]))

(defn tpsc [style]
  (let [data @(rf/subscribe [::subs/pdt-count])
        ch @(rf/subscribe [::subs/chambers [0]])
        sc (get-in ch [:section-count])
        pc (get-in ch [:peep-door-count])
        n (/ pc sc)]
    [:div
     [:label (use-sub-style style :form-label)
      (translate [:config :peep-door-tube-count :label]
                 "No of tubes for each peep door in one section")]
     (into [:div]
           (mapv (fn [i]
                   [form-cell-3 style (:error (get data i))
                    (text-input [true (:value (get data i))
                                 ::event/set-pdt-count-sf
                                 [:sf-config :peep-door-tube-count i]])])
                 (range n)))]))

(defn r-name [style path]
  (let [{:keys [:valid? :value :error]}
        @(rf/subscribe [::subs/ch-fields [path :side-names 1]])]
    [form-cell-3 style error
     (translate [:config :chamber :right-side-name :label] "Name- right side")
     (text-input [valid? value ::event/set-field
                  [:sf-config :chambers path :side-names 1]])]))

(defn l-name [style path]
  (let [{:keys [:valid? :value :error]}
        @(rf/subscribe [::subs/ch-fields [path :side-names 0]])]
    [form-cell-3 style error
     (translate [:config :chamber :left-side-name :label] "Name- left side")
     (text-input [valid? value ::event/set-field
                  [:sf-config :chambers path :side-names 0]])]))

(defn burner-numbering [style path]
  (let [options @(rf/subscribe [::subs/start-end-options :burner-count-per-row])
        value @(rf/subscribe [::subs/selected-start-end [path] :burner-count])
        selected (some #(if (= (:id %) value) %) options)]
    [form-cell-3 style nil
     (translate [:config :chamber :burner-numbering :label] "Burners")
     (select-input [true options selected ::event/set-start-end-options
                    [:sf-config :chambers path :burner-count] :id :name])]))

(defn tube-numbering [style path]
  (let [options @(rf/subscribe [::subs/start-end-options :tube-count])
        value @(rf/subscribe [::subs/selected-start-end [path] :tube-count])
        selected (some #(if (= (:id %) value) %) options)]
    [form-cell-3 style nil
     (translate [:config :chamber :tube-numbering :label] "Tubes")
     (select-input [true options selected ::event/set-start-end-options
                    [:sf-config :chambers path :tube-count] :id :name])]))

(defn ch-name [style path]
  (let [{:keys [:valid? :value :error]}
        @(rf/subscribe [::subs/ch-fields [path :name]])]
    [form-cell-3 style error
     (translate [:config :chamber :name :label] "Name")
     (text-input [valid? value ::event/set-field
                  [:sf-config :chambers path :name]])]))

(defn chamber [style path]
  [:div
   [:label (use-sub-style style :form-heading-label)
    (translate [:config :chamber :label]
               (str "Chamber #" (inc path)))] [:br]
   [ch-name style path]
   [tube-numbering style path]
   [burner-numbering style path]
   [l-name style path]
   [r-name style path]])

(defn chambers [style]
  [:div
   [chamber style 0]
   (let [chs @(rf/subscribe [::subs/ch-count])]
     (if (= chs 2) [chamber style 1]))])

(defn whs-position [style]
  (let [chs @(rf/subscribe [::subs/ch-count])]
    (if (= chs 1)
      [form-cell-2 style nil
       (translate [:config :placement-of-WHS? :label] "Placement of WHS")
       (let [val @(rf/subscribe [::subs/config [:placement-of-WHS]])]
         (app-comp/selector
           {:item-width 40
            :selected   val
            :options    ["end" "side" "roof"]
            :on-select  #(rf/dispatch [::event/set-field [:sf-config :placement-of-WHS] %])}))])))

(defn form-sf [style]
  [:div
   [rf-name style]
   [dual-chamber style]
   [dual-nozzle style]
   [whs-position style] [:br]
   [:label (use-sub-style style :form-heading-label)
    (translate [:config :chamber-configuration :label] "Chamber configuration")]
   [:div
    ;;tube count
    [ch-common-field style
     (translate [:config :tube-count :label] "No of tubes")
     :tube-count]

    ;;burner-row-count
    [ch-common-field style
     (translate [:config :burner-row-count :label] "No of burner rows")
     :burner-row-count]

    ;;burner count per row
    [ch-common-field style
     (translate [:config :burner-count-per-row :label] "No of burners per row")
     :burner-count-per-row]

    ;;peep door count
    [ch-common-field style
     (translate [:config :peep-door-count :label] "No of peepdoors")
     :peep-door-count]

    ;;section count
    [ch-common-field style
     (translate [:config :peep-door-count :label] "No of sections")
     :section-count]]
   [tpsc style]
   [chambers style]])

(defn tbps [style label key]
  (let [sc @(rf/subscribe [::subs/config [:section-count]])]
    (if sc
      [form-cell-4 style nil
       label
       (into [:div]
             (mapv (fn [i]
                     (let [{:keys [:valid? :value :error]}
                           @(rf/subscribe [::subs/section-rows-tf i key])]
                       [form-cell-3 style error
                        (text-input [valid? value ::event/set-field [:tf-config :sections i key]])]))
                   (range sc)))])))

(defn burner-first [style]
  [form-cell-4 style nil
   (translate [:config :burner-first? :label] "Burners located between outer tube rows and furnace walls")
   (toggle-input [@(rf/subscribe [::subs/config [:burner-first?]])
                  ::event/set-burner-first [:tf-config :burner-first?]])])

(defn trc [style]
   (let [{:keys [:value :error :valid?]}
         @(rf/subscribe [::subs/config-field-tf [:tube-row-count]])]
     [form-cell-4 style error
      (translate [:config :tube-row-count :label] "Number of tube rows")
      (text-input [valid? value ::event/set-tube-row-count
                  [:tf-config :tube-row-count] {:required? true
                                                :number true}])]))

(defn tcpr [style]
  (let [{:keys [:value :error :valid?]}
        @(rf/subscribe [::subs/tube-count-tf])]
    [form-cell-4 style error
     (translate [:config :tube-rows :label] "Number of tubes per tube row")
     (text-input [valid? value ::event/set-tb-rows-tf
                  :tube-count nil])]))

(defn bcpr [style]
   (let [{:keys [:value :error :valid?]}
         @(rf/subscribe [::subs/burner-count-tf])]
     [form-cell-4 style error
      (translate [:config :burner-rows :label] "Number of burners in each individual burner row")
      (text-input [valid? value ::event/set-tb-rows-tf
                   :burner-count nil])]))

(defn sc [style]
   (let [{:keys [:value :error :valid?]}
         @(rf/subscribe [::subs/config-field-tf [:section-count]])]
     [form-cell-4 style error
      (translate [:config :section-count :label] "Number of sections per tube row")
      (text-input [valid? value ::event/set-section-count-tf [:section-count]])]))

(defn measure-level [style]
  [:div
   [form-cell-3 style nil
    (translate [:config :top? :label] "Top")
    (toggle-input [@(rf/subscribe [::subs/config [:measure-levels :top?]])
                   ::event/set-field [:tf-config :measure-levels :top?]])]

   [form-cell-3 style nil
    (translate [:config :middle? :label] "Middle")
    (toggle-input [@(rf/subscribe [::subs/config [:measure-levels :middle?]])
                   ::event/set-field [:tf-config :measure-levels :middle?]])]

   [form-cell-3 style nil
    (translate [:config :bottom? :label] "Bottom")
    (toggle-input [@(rf/subscribe [::subs/config [:measure-levels :bottom?]])
                   ::event/set-field [:tf-config :measure-levels :bottom?]])]])

(defn tb-numbering [style count-path row-path label]
  (let [options @(rf/subscribe [::subs/start-end-options count-path])
        n-row (count (:value @(rf/subscribe
                                [::subs/config-field-tf
                                 [row-path]])))
        tc (:value @(rf/subscribe [::subs/tube-count-tf]))
        bc (:value @(rf/subscribe [::subs/burner-count-tf]))]

    (if (and n-row (or tc bc))
      (into [:div
             [:label (use-sub-style style :form-label)
              (translate [:config :numbering :label] label)] [:br]]
            (mapv (fn [i]
                    (let []
                      [form-cell-3 style nil
                       (translate [:config :rows :label] (str "Row " (inc i)))
                       (select-input [true options nil ::event/set-start-end-options
                                      [:tf-config row-path i count-path] :id :name])]))
                  (range n-row))))))

(defn form-tf [style]
  [:div
   [rf-name style]
   [burner-first style]
   [trc style]
   [tcpr style]
   [bcpr style]
   [tb-numbering style :tube-count :tube-rows "Tubes:"]
   [tb-numbering style :burner-count :burner-rows "Burners:"]
   [sc style]
   [tbps style
    (translate [:config :tube-rows :label] "Distribution of tubes per section")
    :tube-count]
   [tbps style
    (translate [:config :burner-rows :label] "Distribution of burners per section")
    :burner-count]
   [:label (use-sub-style style :form-label)
    (translate [:config :measure-levels :label] "Specify peephole levels:")] [:br]
   [measure-level style]])

(defn form [style]
  (let [{:keys [:value :error :valid?]} @(rf/subscribe [::subs/firing])
        options @(rf/subscribe [::subs/firing-opts])
        selected (some #(if (= (:id %) value) %) options)]
    [:div
     [form-cell-2 style nil
      (translate [:config :reformer-type :label] "Reformer type")
      [app-comp/dropdown-selector
       {:valid?    valid?
        :items     options
        :selected  selected
        :on-select #(rf/dispatch [::event/set-field [:firing] (:id %)])
        :value-fn  :id, :label-fn :name}]]
     (case value
       "side" [form-sf style]
       "top" [form-tf style])]))

(defn body [{:keys [width height]}]
  (let [w (* (- width 85) 0.4)
        h (- height 40)
        style (style/body width height)
        config-key (case (:value @(rf/subscribe [::subs/firing]))
                 "side" :tf-config
                 "top" :sf-config)]
    [:div (use-style style)
     [scroll-box (use-sub-style style :form-scroll)
      [form style]]
     [app-view/vertical-line {:height h}]
     [reformer-dwg {:width  w
                    :height h
                    :config (dissoc @(rf/subscribe [::subs/data]) config-key)}]]))

(defn config [props]
  [app-view/layout-main
   (translate [:settings :title :text] "Configuration")
   (translate [:settings :title :sub-text] "Reformer configuration")
   [[app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
                      :icon ic/save
                      :label (translate [:action :save :label] "Save")
                      :on-click #(rf/dispatch [::event/save])}]
    [app-comp/button {:icon ic/cancel
                      :label (translate [:action :cancel :label] "Cancel")
                      :on-click #(rf/dispatch [::root-event/activate-content :home])}]]
   body])