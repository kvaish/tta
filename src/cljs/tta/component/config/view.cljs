;; view elements component config
(ns tta.component.config.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.config.style :as style]
            [tta.component.config.subs :as subs]
            [tta.component.config.event :as event]
            [tta.component.reformer-dwg.view :refer [reformer-dwg]]))

(defn- text-field [id label type validations]
  (let [field-path (conj [] (keyword id))
        sub @(rf/subscribe [::subs/get-field field-path])]
    [ui/text-field
     {:on-change #(rf/dispatch [::event/set-field field-path %2 validations])
      :default-value (:value sub)
      :hint-text label
      :error-text (:error sub)
      :floating-label-text label
      :name id
      :type type
      :label label}]))

(defn- select-field [id label options]
  (let [field-path (conj [] (keyword id))]
    (into
      [ui/select-field
       {:value (:value @(rf/subscribe [::subs/get-field field-path]))
        :on-change #(rf/dispatch [::event/set-field field-path %3])
        :floating-label-text label
        :name id
        :label label}
       (map (fn [{:keys [id name]}]
              [ui/menu-item {:key id
                             :value id
                             :primary-text name}])
            options)])))

(defn checkbox-field [id label]
  [ui/checkbox
   {:on-check #(rf/dispatch [::event/set-field [id] %2])
    :value    (:value @(rf/subscribe [::subs/get-field [id]]))
    :label    label
    :name     (name id)
    }])

(defn pd-per-section []
  (let [pd-count @(rf/subscribe [::subs/get-field [:peep-door-count]])
        section-count @(rf/subscribe [::subs/get-field [:section-count]])
        npd (/ pd-count section-count)]
    ;(js/console.log)
    (into (map (fn [n]
                 [text-field (keyword (str "pd" n)) ""]) (range npd)))
    [:p]))

(defn chamber []
  (let [ch (:value @(rf/subscribe [::subs/get-field [:sf-config :chambers]]))]
    (js/console.log ch)
    [:div]
    #_(map (fn [c]
           [:div
            [:label "Chamber"]
            [:label "Name"] [text-field :name "Name"]
            [select-field :tube-numbering "Tubes"
             [{}]]]) ch)))

(defn form-sf []
  [:div
   [text-field :name "Reformer Name"]
   [checkbox-field :dual-chamber? "Dual Chamber"]
   [checkbox-field :dual-nozzle? "Dual fuel nozzle"]
   [text-field :tube-count "No of tubes"
    "number"
    {:required? true
     :number {:decimal? false}}]
   [text-field :burner-row-count "No of Burner rows"
    "number"
    {:required? true
     :number {:decimal? false}}]
   [text-field :burner-count-per-row "No of burners per row"
    "number"
    {:required? true
     :number {:decimal? false}}]
   [text-field :peep-door-count "No of peep doors"
    "number"
    {:required? true
     :number {:decimal? false}}]
   [text-field :section-count "No of sections"
    "number"
    {:required? true
     :number {:decimal? false}}]
   [pd-per-section]
   [chamber]])

(defn form-tf []
  [:div
   [:label "Burners located between outer tube rows and furnace walls"]
   [checkbox-field :burner-first? ""] [:br]
   [:label "Number of tube rows"]
   [text-field :tube-row-count ""
    "number"
    {:required? true
     :number {:decimal? false}}] [:br]
   [:label "Number of tubes per tube row"]
   [text-field :tube-count ""
    "number"
    {:required? true
     :number {:decimal? false}}] [:br]
   [:label "Number of burners in each individual row"]
   [text-field :burner-row-count ""
    "number"
    {:required? true
     :number {:decimal? false}}] [:br]
   [:label "Number of sections per tube row"]
   [text-field :section-count ""
    "number"
    {:required? true
     :number {:decimal? false}}] [:br]
   ;;[tubes-per-section]
   ;[burners-per-section]
   [:label "Specify peephole levels:"]
   [checkbox-field :top "Top"]
   [checkbox-field :middle "Middle"]
   [checkbox-field :bottom "Bottom"]])

(defn form []
  [ui/card
   [select-field :firing ;(translate [:configuration :type :label])
    "Reformer Type"
    [{:id "side" :name "Side Fired" }
     {:id "top" :name "Top Fired"}]]
   [ui/divider]
   (let [type @(rf/subscribe [::subs/get-field [:firing]])]
     (if type (case (:value type)
                "side" [form-sf]
                "top" [form-tf])))])

(defn config [props]
  [:div {:style {:height "inherit" :width "inherit"}}
   ;[:div (use-style style/menu)]
   [:span {:style {:float "right" :margin "10px"}}
    [ui/raised-button {:on-click #()
                       :style    {:margin-right "10px"}} "SAVE"]
    [ui/raised-button {:on-click #()
                       :primary  true
                       :style    {:margin-right "10px"}} "CANCEL"]]
   [:div (use-style style/container)
    [:div (use-style style/form)
     #_[form]]
    [:div (use-style style/sketch)
     [reformer-dwg]]]])