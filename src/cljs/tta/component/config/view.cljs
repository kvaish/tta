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
            [tta.dialog.view-factor.view :refer [view-factor]]
            [tta.component.reformer-dwg.view :refer [reformer-dwg]]
            [clojure.string :as str]))


(defn show-error? [] @(rf/subscribe [::subs/show-error?]))

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

(defn form-cell-4
  ([style label widgets] (form-cell style :form-cell-4 label widgets))
  ([style error label widget] (form-cell style :form-cell-4 error label widget)))

(defn form-cell-4x3
  ([style label widgets] (form-cell style :form-cell-4x3 label widgets))
  ([style error label widget] (form-cell style :form-cell-4x3 error label widget)))

(defn query-id [query-id & params]
  (let [{:keys [value error valid?]} @(rf/subscribe (into [query-id] params))
        show-err? (show-error?)]
    {:value value
     :error (if show-err? (if (fn? error) (error) error))
     :valid? (if show-err? valid? true)}))

(defn query-fn [query-fn & params]
  (let [{:keys [value error valid?]} @(apply query-fn params)
        show-err? (show-error?)]
    {:value value
     :error (if show-err? (if (fn? error) (error) error))
     :valid? (if show-err? valid? true)}))

;; side-fired ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sf-dual-chamber? [style]
  (let [dual? @(rf/subscribe [::subs/sf-dual-chamber?])]
    [form-cell-2 style nil
     (translate [:config :dual-chamber? :label] "Dual chamber?")
     [app-comp/toggle
      {:value dual?
       :on-toggle #(rf/dispatch [::event/set-sf-dual-chamber? %])}]]))

(defn sf-dual-nozzle? [style]
  (let [dual? @(rf/subscribe [::subs/sf-dual-nozzle?])]
    [form-cell-2 style nil
     (translate [:config :dual-nozzle? :label] "Dual nozzle?")
     [app-comp/toggle
      {:value dual?
       :on-toggle #(rf/dispatch [::event/set-sf-dual-nozzle? %])}]]))

(defn sf-whs-placement [style]
  (let [dual? @(rf/subscribe [::subs/sf-dual-chamber?])
        opts [{:id "end", :label (translate [:config :whs-place :end] "End")}
              {:id "side", :label (translate [:config :whs-place :side] "Side")}
              {:id "roof", :label (translate [:config :whs-place :roof] "Roof")}]
        sel @(rf/subscribe [::subs/sf-placement-of-WHS])
        selected (some #(if (= (:id %) sel) %) opts)]
    (if-not dual?
      [form-cell-2 style nil
       (translate [:config :placement-of-whs :label] "WHS location")
       [app-comp/selector
        {:options opts, :label-fn :label, :selected selected
         :on-select #(rf/dispatch [::event/set-sf-placement-of-WHS (:id %)])}]])))

(defn sf-tube-count [style]
  (let [{:keys [value error valid?]} (query-id ::subs/sf-tube-count-field)]
    [form-cell-1 style error
     (translate [:config :tube-count :label] "No of tubes")
     [:span
      [app-comp/text-input
       {:value value, :valid? valid?, :align "center"
        :on-change #(rf/dispatch [::event/set-sf-tube-count %])}]]]))

(defn sf-burner-row-count [style]
  (let [{:keys [value error valid?]} (query-id ::subs/sf-burner-row-count-field)]
    [form-cell-2 style error
     (translate [:config :burner-row-count :label] "No of burner rows")
     [:span
      [app-comp/text-input
       {:value value, :valid? valid?, :align "center"
        :on-change #(rf/dispatch [::event/set-sf-burner-row-count %])}]]]))

(defn sf-burner-count-per-row [style]
  (let [{:keys [value error valid?]} (query-id ::subs/sf-burner-count-per-row-field)]
    [form-cell-2 style error
     (translate [:config :burner-count-per-row :label] "No of burners per row")
     [:span
      [app-comp/text-input
       {:value value, :valid? valid?, :align "center"
        :on-change #(rf/dispatch [::event/set-sf-burner-count-per-row %])}]]]))

(defn sf-peep-door-count [style]
  (let [{:keys [value error valid?]} (query-id ::subs/sf-pd-count-field)]
    [form-cell-1 style error
     (translate [:config :peep-door-count :label] "No of peep doors")
     [:span
      [app-comp/text-input
       {:value value, :valid? valid?, :align "center"
        :on-change #(rf/dispatch [::event/set-sf-pd-count %])}]]]))

(defn sf-section-count [style]
  (let [{:keys [value error valid?]} (query-id ::subs/sf-section-count-field)]
    [form-cell-2 style error
     (translate [:config :section-count :label] "No of sections")
     [:span
      [app-comp/text-input
       {:value value, :valid? valid?, :align "center"
        :on-change #(rf/dispatch [::event/set-sf-section-count %])}]]]))

(defn sf-peep-door-tube-count [style index]
  (let [{:keys [value valid?]} (query-id ::subs/sf-pd-tube-count-field index)]
    [app-comp/text-input
     {:value value, :valid? valid?, :align "center", :width 60
      :on-change #(rf/dispatch [::event/set-sf-pd-tube-count index %])}]))

(defn sf-peep-doors [style]
  (if-let [n @(rf/subscribe [::subs/sf-pd-count-per-section])]
    [form-cell-1 style
     (translate [:config :peep-door-tube-count :label]
                "No of tubes in each peep door")
     (map #(with-meta (vector sf-peep-door-tube-count style %) {:key %})
          (range n))]))

(defn sf-chamber-validity [style]
  (let [{:keys [error]} (query-id ::subs/sf-chamber-validity)]
    (if error
      [:div (use-sub-style style :div-error) error])))

(defn sf-chamber-name [style index]
  (let [{:keys [value error valid?]} (query-fn subs/sf-chamber-name-field index)]
    [form-cell-1 style error
     (translate [:config :chamber-name :label] "Chamber name")
     [app-comp/text-input
      {:value value, :valid? valid?, :width 150
       :on-change (partial event/set-sf-chamber-name index)}]]))

(defn sf-side-name [style ch-index s-index]
  (let [{:keys [value error valid?]} (query-fn subs/sf-side-name-field ch-index s-index)]
    [form-cell-2 style error
     (str (translate [:config :side-name :label] "Side name") " #" (inc s-index))
     [app-comp/text-input
      {:value value, :valid? valid?, :width 100
       :on-change (partial event/set-sf-side-name ch-index s-index)}]]))

(defn sf-tube-numbers [style index]
  (let [options @(rf/subscribe [::subs/sf-tube-numbers-options])
        sel @(rf/subscribe [::subs/sf-tube-numbers-selection index])
        selected (some #(if (= (:id %) sel) %) options)]
    (if-not (empty? options)
      [form-cell-2 style nil
       (translate [:config :tube-numbers :label] "Tube numbers")
       [app-comp/dropdown-selector
        {:items options, :selected selected
         :width 72, :value-fn :id, :label-fn :label
         :on-select #(rf/dispatch [::event/set-sf-tube-numbers-selection
                                   index (:id %)])}]])))

(defn sf-burner-numbers [style index]
  (let [options @(rf/subscribe [::subs/sf-burner-numbers-options])
        sel @(rf/subscribe [::subs/sf-burner-numbers-selection index])
        selected (some #(if (= (:id %) sel) %) options)]
    (if-not (empty? options)
      [form-cell-2 style nil
       (translate [:config :burner-numbers :label] "Burner numbers")
       [app-comp/dropdown-selector
        {:items options, :selected selected
         :width 72, :value-fn :id, :label-fn :label
         :on-select #(rf/dispatch [::event/set-sf-burner-numbers-selection
                                   index (:id %)])}]])))

(defn sf-chambers [style]
  (let [n (if @(rf/subscribe [::subs/sf-dual-chamber?]) 2 1)]
    (->> (range n)
         (map (fn [index]
                [:div (use-sub-style style :form-cell-1)
                 [:div (use-sub-style style :form-heading-label)
                  "Chamber #" (inc index)]
                 [sf-chamber-name style index]
                 (map #(with-meta (vector sf-side-name style index %) {:key %})
                      (range 2))
                 [sf-tube-numbers style index]
                 [sf-burner-numbers style index]]))
         (into [:div]))))

(defn sf-form [style]
  [:div
   [:div
    [sf-dual-chamber? style]
    [sf-whs-placement style]]
   [sf-dual-nozzle? style]
   [:div (use-sub-style style :form-heading-label)
    (translate [:config :chamber-configuration :label] "Chamber configuration")]
   [:div
    [sf-tube-count style]
    [sf-burner-row-count style]
    [sf-burner-count-per-row style]
    [sf-peep-door-count style]]
   [:div
    [sf-section-count style]
    [sf-peep-doors style]]
   [sf-chamber-validity style]
   [sf-chambers style]])

;; top-fired ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tf-burner-first [style]
  (let [first? @(rf/subscribe [::subs/tf-burner-first?])]
    [form-cell-1 style nil
     (translate [:config :burner-first? :label]
                "Are there burners between outer tube rows and furnace walls?")
     [app-comp/toggle
      {:value first?
       :on-toggle #(rf/dispatch [::event/set-tf-burner-first? %])}]]))

;; TF TUBES

(defn tf-tube-row-count [style]
  (let [{:keys [value error valid?]} (query-id ::subs/tf-tube-row-count-field)]
    [form-cell-1 style error
     (translate [:config :tube-row-count :label]
                "No of tube rows")
     [app-comp/text-input
      {:value value, :valid? valid?, :align "center"
       :on-change #(rf/dispatch [::event/set-tf-tube-row-count %])}]]))

(defn tf-tube-count-per-row [style]
  (let [{:keys [value error valid?]} (query-id ::subs/tf-tube-count-per-row-field)]
    [form-cell-1 style error
     (translate [:config :tube-count-per-row :label] "No of tubes per row")
     [app-comp/text-input
      {:value value, :valid? valid?, :align "center"
       :on-change #(rf/dispatch [::event/set-tf-tube-count-per-row %])}]]))

(defn tf-tube-numbers-selection [style index]
  (let [{:keys [value error valid?]} (query-id ::subs/field
                                               [:tf-config :tube-rows index :name])
        sel-id @(rf/subscribe [::subs/tf-tube-numbers-selection index])
        options @(rf/subscribe [::subs/tf-tube-numbers-options])
        selected (some #(if (= (:id %) sel-id) %) options)]

    [:div (use-sub-style style :form-cell-1)
     [:div (use-sub-style style :form-heading-label)
      (str (translate [:config :row :name] "Row #") (inc index))]
     [:div
      [form-cell-2 style nil
       (translate [:config :row :name] "Tube number")
       [app-comp/dropdown-selector
        {:items options, :selected selected
         :width 72, :value-fn :id, :label-fn :label
         :on-select #(rf/dispatch [::event/set-tf-tube-numbers-selection
                                   index (:id %)])}]]
      [form-cell-2 style error
       (translate [:config :row :name] "Row Name")
       [app-comp/text-input
        {:value value, :valid? valid?, :width 100
         :on-change #(rf/dispatch [::event/set-text
                                   [:tf-config :tube-rows index :name] % true])}]]]]))


(defn tf-tube-numbers [style]
  (let [rn @(rf/subscribe [::subs/tf-tube-row-count])
        tn @(rf/subscribe [::subs/tf-tube-count-per-row])]
    (if (and rn tn)
      (doall
       (map #(with-meta (vector tf-tube-numbers-selection style %) {:key %})
            (range rn))))))

;; TF BURNERS

(defn tf-burner-count-per-row [style]
  (let [{:keys [value error valid?]} (query-id ::subs/tf-burner-count-per-row-field)]
    [form-cell-1 style error
     (translate [:config :burner-count-per-row :label] "No of burners per row")
     [app-comp/text-input
      {:value value, :valid? valid?, :align "center"
       :on-change #(rf/dispatch [::event/set-tf-burner-count-per-row %])}]]))

(defn tf-burner-numbers-selection [index]
  (let [sel-id @(rf/subscribe [::subs/tf-burner-numbers-selection index])
        options @(rf/subscribe [::subs/tf-burner-numbers-options])
        selected (some #(if (= (:id %) sel-id) %) options)]
    [app-comp/dropdown-selector
     {:items options, :selected selected
      :width 72, :value-fn :id, :label-fn :label
      :on-select #(rf/dispatch [::event/set-tf-burner-numbers-selection
                                index (:id %)])}]))

(defn tf-burner-numbers [style]
  (let [rn @(rf/subscribe [::subs/tf-burner-row-count])
        bn @(rf/subscribe [::subs/tf-burner-count-per-row])]
    (if (and rn bn)
      [form-cell-1 style
       (translate [:config :burner-numbers :label] "Burner numbers")
       (map #(with-meta (vector tf-burner-numbers-selection %) {:key %})
            (range rn))])))

;;TF SIDE NAMES
(defn tf-wall-name [style side]
  (let [{:keys [value error valid?]} (query-id ::subs/tf-wall-name-field side)]
    [form-cell-2 style error
     (str (translate [:config :wall-name :label] (str/capitalize (name side))))
     [app-comp/text-input
      {:value value, :valid? valid?, :width 100
       :on-change #(rf/dispatch [::event/set-tf-wall-name side %])}]]))

(defn tf-side-names [style]
  (let [sides [:east :west :north :south]] 
    (doall
     (map #(with-meta (vector tf-wall-name style %) {:key %}) sides))))

;; TF SECTIONS

#_(defn tf-section-count [style]
    (let [{:keys [value error valid?]} (query-id ::subs/tf-section-count-field)]
      [form-cell-1 style error
       (translate [:config :section-count :label]
                  "No of sections")
       [app-comp/text-input
        {:value value, :valid? valid?, :align "center"
         :on-change #(rf/dispatch [::event/set-tf-section-count %])}]]))

#_(defn tf-section-tube-count [index]
    (let [{:keys [value valid?]} (query-id ::subs/tf-section-tube-count-field index)]
      [app-comp/text-input
       {:value value, :valid? valid?, :align "center", :width 60
        :on-change #(rf/dispatch [::event/set-tf-section-tube-count index %])}]))

#_(defn tf-section-tubes [style]
    (let [sn @(rf/subscribe [::subs/tf-section-count])
          tn @(rf/subscribe [::subs/tf-tube-count-per-row])]
      (if (and sn tn)
        [form-cell-1 style
         (translate [:config :section-tube-counts :label]
                    "No of tubes in each section")
         (map #(with-meta (vector tf-section-tube-count %) {:key %})
              (range sn))])))

#_(defn tf-section-burner-count [index]
    (let [{:keys [value valid?]} (query-id ::subs/tf-section-burner-count-field index)]
      [app-comp/text-input
       {:value value, :valid? valid?, :align "center", :width 60
        :on-change #(rf/dispatch [::event/set-tf-section-burner-count index %])}]))

#_(defn tf-section-burners [style]
    (let [sn @(rf/subscribe [::subs/tf-section-count])
          tn @(rf/subscribe [::subs/tf-burner-count-per-row])]
      (if (and sn tn)
        [form-cell-1 style
         (translate [:config :section-burner-counts :label]
                    "No of burners in each section")
         (map #(with-meta (vector tf-section-burner-count %) {:key %})
              (range sn))])))

#_(defn tf-sections-validity [style]
    (let [{:keys [error]} (query-id ::subs/tf-sections-validity)]
      (if error
        [:div (use-sub-style style :div-error) error])))

;; MEASURE LEVELS

(defn tf-toggle-level [style {:keys [key label]}]
  (let [chk? @(rf/subscribe [::subs/tf-measure-level? key])]
    [form-cell-1 style nil label
     [app-comp/toggle
      {:value chk?
       :on-toggle #(rf/dispatch [::event/set-tf-measure-level? key %])}]]))

(defn tf-measure-levels [style]
  (let [{:keys [error]} (query-id ::subs/tf-measure-levels-validity)
        levels [{:key :top?
                 :label (translate [:measure-level :top? :label] "Top?")}
                {:key :middle?
                 :label (translate [:measure-level :middle? :label] "Middle?")}
                {:key :bottom?
                 :label (translate [:mesaure-level :bottom? :label] "Bottom?")}]]
    [form-cell-1 style error
     (translate [:config :select-levels :label] "Select levels to measure")
     (into [:div]
           (map #(vector tf-toggle-level style %) levels))]))

(defn tf-form [style]
  [:div
   [tf-burner-first style]
   [tf-tube-row-count style]
   (if @(rf/subscribe [::subs/tf-tube-row-count])
     [:div
      [:div (use-sub-style style :form-heading-label)
       (translate [:config :tube-rows :label] "Tubes rows")]
      [tf-tube-count-per-row style]
      (tf-tube-numbers style)
      [:div (use-sub-style style :form-heading-label)
       (translate [:config :wall-names :label] "Wall Names")]
      (tf-side-names style)
      [:div (use-sub-style style :form-heading-label)
       (translate [:config :burner-rows :label] "Burner rows")]
      [tf-burner-count-per-row style]
      [tf-burner-numbers style]
      #_[:div (use-sub-style style :form-heading-label)
         (translate [:config :sections :label] "Sections")]
                                        ;    [tf-section-count style]
                                        ;   [tf-section-tubes style]
      ])
                                        ;[tf-section-burners style]
                                        ;[tf-sections-validity style]
   [:div (use-sub-style style :form-heading-label)
    (translate [:config :measure-levels :label] "Measurement levels")]
   [tf-measure-levels style]])

;;; Reformer ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn reformer-name [style]
  (let [{:keys [value error valid?]} (query-fn subs/reformer-name-field)]
    [form-cell-1 style error
     (translate [:config :name :label] "Reformer name")
     [app-comp/text-input {:valid? valid?
                           :value value
                           :width 200
                           :on-change event/set-reformer-name}]]))

(defn firing-type [style]
  (let [{:keys [value error valid?]} (query-fn subs/firing-field)
        options @(rf/subscribe [::subs/firing-options])
        selected (some #(if (= (:id %) value) %) options)]
    [form-cell-1 style error
     (translate [:config :type :label] "Reformer type")
     [app-comp/dropdown-selector
      {:valid?    valid?
       :items     options
       :selected  selected
       :width     150
       :on-select #(rf/dispatch [::event/set-firing (:id %)])
       :value-fn  :id, :label-fn :name}]]))

(defn form [style]
  [:div
   [:div (use-sub-style style :form-heading-label)
    (translate [:config :reformer :label] "Reformer configuration")]
   [firing-type style]
   [reformer-name style]
   ;; firing specific reformer form
   (case @(rf/subscribe [::subs/firing])
     "top" [tf-form style]
     "side" [sf-form style]
     ;; none selected
     nil)])

(defn body [{:keys [width height]}]
  (let [w (* (- width 85) 0.5)
        h (- height 40)
        style (style/body width height)
        data @(rf/subscribe [::subs/data])]
    [:div (use-style style)
     [scroll-box (assoc (use-sub-style style :form-scroll)
                        :*-force-render data)
      [form style]]
     [app-view/vertical-line {:height h}]
     [reformer-dwg {:width  w, :height h
                    :config @(rf/subscribe [::subs/sketch-config])}]
     ;;dialogs
     (if @(rf/subscribe [:tta.dialog.view-factor.subs/open?])
       [view-factor])]))

(defn config [props]
  (let [firing @(rf/subscribe [::subs/firing])]
    [app-view/layout-main
     (translate [:config :title :text] "Configuration")
     (translate [:config :title :sub-text] "Reformer configuration")
     [(if (= "top" firing)
        [app-comp/button {:disabled? false
                          :icon ic/upload
                          :label (translate [:action :view-factor :label]
                                            "View Factor")
                          :on-click #(rf/dispatch
                                      [:tta.dialog.view-factor.event/open])}])
      
      [app-comp/button {:disabled? (if (show-error?)
                                     (not @(rf/subscribe [::subs/can-submit?]))
                                     (not @(rf/subscribe [::subs/dirty?])))
                        :icon ic/upload
                        :label (translate [:action :upload :label] "Upload")
                        :on-click #(rf/dispatch [::event/upload])}]
      [app-comp/button {:icon ic/cancel
                        :label (translate [:action :cancel :label] "Cancel")
                        :on-click #(rf/dispatch [::root-event/activate-content :home])}]]
     body]))
