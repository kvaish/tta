;; view elements dialog edit-pyrometer
(ns tta.dialog.edit-pyrometer.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [ht.util.interop :as i]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.core :as ui-core]
            [cljs-time.core :as clj-date]
            [ht.app.comp :as ht-comp]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.app.view :as app-view]
            [tta.dialog.edit-pyrometer.style :as style]
            [tta.dialog.edit-pyrometer.subs :as subs]
            [tta.dialog.edit-pyrometer.event :as event]))


(defn- text-field [id label type pyrometer-id validations]
  (let [field-path (conj [] (keyword id))
        field @(rf/subscribe [::subs/get-pyrometer-field field-path pyrometer-id])]
    (print field)
    [ui/text-field 
     {:on-change #(rf/dispatch [::event/set-field field-path %2 pyrometer-id  validations])
      :default-value
      (if (= type "date")
        (js/Date (:value field))            
        (:value  field))
      :id (str id pyrometer-id) 
      :hint-text label
      :error-text (:error field)
      #_:floating-label-text #_label
      :floating-label-fixed true
      :style {:width "150px"
              :margin "5px"}
      :name (str id pyrometer-id) 
      :type type
      :label label}]))

(defn edit-popover [props]
  (let [anchor-el (:menu @props)
        id (:id @props)]
    [ui/popover
     {:open (if (nil? @(rf/subscribe [::subs/popover-open?])) false
                @(rf/subscribe [::subs/popover-open?]))
      :desktop true
      :anchor-el anchor-el
      :anchor-origin {:horizontal "left"
                      :vertical "bottom"}
      :target-origin {:horizontal "left"
                      :vertical "bottom"}
      :use-layer-for-click-away false
      :animated true
      :auto-close-when-off-screen false
      :on-request-close #(rf/dispatch [::event/discard-popover-data id])
      ;;:on-click-away  #(rf/dispatch [::event/discard-popover-data id])
      } 
     [ui/menu]
     [:div (merge (use-style style/row) {:class "row"})
      (text-field "name"
                  (translate [:pyrometerDialog :name :label]
                             "Name")
                  "text"
                  id
                  {:required? true})
      (text-field "serial-number"
                  (translate [:pyrometerDialog :serial-number :label]
                             "Serial Number")
                  "text"
                  id
                  {:required? true})
      (text-field "wavelength"
                  (translate [:pyrometerDialog :wavelength :label]
                             "Wavelength")
                  "number"
                  id
                  {:required? true
                   :number {:min 0
                            :max 100
                            :decimal false}})
      (text-field "date-of-calibration"
                  (translate [:pyrometerDialog :weblength :label]
                             "Date of Calibration")
                  "date"
                  id
                  [:required])
      
      [ui/icon-button
       {:icon-class-name "fa fa-close"
        :on-click #(rf/dispatch [::event/discard-popover-data id])}]

      [ui/icon-button
       {:icon-class-name "fa fa-save"
        :disabled (not @(rf/subscribe [::subs/valid-dirty? id]))
        :on-click #(rf/dispatch [::event/save-popover-data id])}]
      ]]))


(defn edit-pyrometer []
  (let [anchor (atom {})]
    (fn []
      (let [open? @(rf/subscribe [::subs/open?])
            data @(rf/subscribe [::subs/pyrometers])
            title (translate [:pyrometerDialog :title] "Manage IR Pyrometer")
            on-close #(rf/dispatch [::event/discard-data])
            close-tooltip  (translate [:pyrometerDialog :close :hint] "Close")]
        
        [ui/dialog
         {:open open?
          :modal false
          :title (r/as-element (ht-comp/optional-dialog-head
                                {:title title
                                 :on-close on-close
                                 :close-tooltip close-tooltip}))
          :auto-scroll-body-content true
          :actions (r/as-element [ui/floating-action-button
                                  {:id "add"
                                   :icon-class-name "fa fa-plus"
                                   :on-click #(do
                                                (i/ocall % :preventDefault)
                                                (swap! anchor assoc  :menu (i/oget % :currentTarget)
                                                       :id "new")
                                                (rf/dispatch [::event/set-popover-open true]))
                                   }])
          }
         [:div
          [ui/list {:class-name "pyrometer-list"}

           (doall (map (fn [{:keys [serial-number date-of-calibration name id wavelength]}]
                  [ui/list-item
                   {:key id
                    :on-click #(do
                                 (i/ocall % :preventDefault)
                                 (swap! anchor assoc  :menu (i/oget % :currentTarget)
                                        :id id)
                                 (rf/dispatch [::event/set-popover-open true]))}
                   [:div (merge (use-style style/row) {:class "row"})
                    [:div (merge (use-style style/col) {:class "col1" })
                     name]
                    [:div (merge (use-style style/col) {:class "col1" })
                     (str (translate [:pyrometerDialog :wavelength :label]
                                     "Wavelength") ":")
                     wavelength]                ]
                   [:div (merge (use-style style/row) {:class "row"})
                    [:div (merge (use-style style/col) {:class "col1" })
                     (str
                      (translate [:pyrometerDialog :serialNumber :label]
                                 "Serial Number") ":") serial-number]
                    [:div (merge (use-style style/col) {:class "col" })
                     (str
                      (translate [:pyrometerDialog :calibrationDate :label]
                                 "Calibration Date") ":") (js/Date
                                                           date-of-calibration)]]]) data))]]
         
         [edit-popover anchor]
         ]))))
