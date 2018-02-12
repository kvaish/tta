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
            [tta.component.reformer-layout.view :refer [reformer-layout reformer-data]]
            [tta.component.reformer-layout-tf.view :refer [reformer-layout-tf reformer-data-tf]]))

(defn sketch []
  (case "side"
    "side" [reformer-layout {:reformer-data @reformer-data}]
    "top" [reformer-layout-tf
           (let [width (+ 50 (* 100 (get-in @reformer-data-tf [:configuration :tf-config :tube-row-count])))]
             {:reformer-data @reformer-data-tf
              :view-box      (str "0 0 " width " 500")
              :svg-width     width
              :height        500
              })]))

(defonce rf-type
         {:side "Side Fired"
          :top "Top Fired"})

(defn text-field [id label]
  [ui/text-field
   {:value               @(rf/subscribe [::subs/get-field [:component :config :data id]])
    :on-change           #(rf/dispatch [::event/set-field id %2])
    :floating-label-text label
    :name                (:name id)}])

(defn select-field [id label options]
  (into
    [ui/select-field
     {:value               @(rf/subscribe [::subs/get-field [:component :config :data id]])
      :on-change           #(rf/dispatch [::event/set-field id %3])
      :floating-label-text label
      :name                (name id)}
     [ui/menu-item {:key "_", :value ""}]]
    (map (fn [o]
           [ui/menu-item {:value o, :primary-text (rf-type (keyword o))}])
         options)))

(defn checkbox-field [id label]
  [ui/checkbox
   {:on-check #(rf/dispatch [::event/set-field id %2])
    :value @(rf/subscribe [::subs/get-field [:component :config :data id]])
    :label label
    :name (name id)
    }])

(defn pd-per-section []
  (let [pd-count @(rf/subscribe [::subs/get-field [:component :config :data :peep-door-count]])
        section-count @(rf/subscribe [::subs/get-field [:component :config :data :section-count]])
        npd (/ pd-count section-count)]
    ;(js/console.log)
    (into (map (fn [n]
                 [text-field (keyword (str "pd" n)) ""]) (range npd)))
    [:p]))

(defn chamber []
  )

(defn form-sf []
  [:div
   [text-field :name "Reformer Name"]
   [checkbox-field :dual-chamber? "Dual Chamber"]
   [checkbox-field :dual-nozzle? "Dual fuel nozzle"]
   [text-field :tube-count "No of tubes"]
   [text-field :burner-row-count "No of Burner rows"]
   [text-field :burner-count-per-row "No of burners per row"]
   [text-field :peep-door-count "No of peep doors"]
   [text-field :section-count "No of sections"]
   [pd-per-section]
   [chamber]]
  )

(defn form-tf []
  [:div
   [:label "Burners located between outer tube rows and furnace walls"]
   [checkbox-field :burner-first? ""] [:br]
   [:label "Number of tube rows"] [text-field :tube-row-count ""] [:br]
   [:label "Number of tubes per tube row"] [text-field :tube-count ""] [:br]
   [:label "Number of burners in each individual row"] [text-field :burner-row-count ""] [:br]
   [:label "Number of sections per tube row"] [text-field :section-count ""] [:br]
   ;;[tubes-per-section]
   ;[burners-per-section]
   [:label "Specify peephole levels:"]
   [checkbox-field :top "Top"]
   [checkbox-field :middle "Middle"]
   [checkbox-field :bottom "Bottom"]])

(defn form []
  [ui/card
   [select-field :type                                      ;(translate [:configuration :type :label])
    "Reformer Type"
    ["side" "top"]]
   [ui/divider]
   (let [type @(rf/subscribe [::subs/get-field [:component :config :data :type]])]
     (if type (case type
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
     [form]]
    [:div (use-style style/sketch)
     [sketch]]]])