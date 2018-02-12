
;; view elements component setting
(ns tta.component.settings.view
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
            [tta.component.settings.style :as style]
            [tta.component.settings.subs :as subs]
            [tta.component.settings.event :as event]))

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

(defn settings [props]
  [:div (use-style style/setting)
   [:div {:class "sub-header"}
    [ui/toolbar
     (merge 
      {:style style/content-toolbar
       :class-name "setting-toolbar"})
     [ui/toolbar-title
      (merge
       {:style style/toolbar-title
        :text (translate [:settings :toolbar :title] "Reformer")})]

     [ui/toolbar-group
      {:last-child true}
      [ui/icon-button
       {:tooltip (translate [:settings :toolbar :save :label] "Save")
        :icon-class-name "fa fa-save"
        :tooltip-position "top-left"
        :icon-style style/toolbar-icon
        :class-name "save"}]
      [ui/icon-button
       {:tooltip (translate
                  [:settings :toolbar :tooltip :close]
                  "Discard Changes and exit.")
        :icon-class-name "fa fa-close"
        :icon-style style/toolbar-icon
        :tooltip-position "top-left"
        :class-name "save"}]]]]

   [:div (use-style style/setting-container)
    [:div (use-style style/reformer-design-container)
     "Reformer design"]
    [:div (use-style style/setting-form-container)
     "Reformer settings"
     [:div {:class "row"}
      (let [temp-unit @(rf/subscribe [::subs/get-field [:temp-unit]])]
        (select-field "temp-unit"
                      (translate [:settings :temp-unit :label]
                                 "Temperature Unit")
                      [{:id "°C" :name "°C" }
                       {:id "°F" :name "°F"}] ))]
     [:div {:class "row"
            :style {:display "flex"}}
      [:div {:class "col"
             :style {:flex 1}}
       (text-field "design-temp" (translate
                                  [:setting :design-temp :label]
                                  "Design Temperature")
                   "number"
                   {:required? true
                    :number {:decimal false}})]
      [:div {:style {:flex 1}}

       (text-field "target-temp" (translate
                                  [:setting :target-temp :label]
                                  "Target Temperature") "number"
                   {:required? true
                    :number {:decimal? false}})]]
     
     [:div {:class "row"
            :style {:display "flex"}}

      [:div {:style {:flex 1}}
       (select-field "pyrometer-id"
                     (translate
                      [:setting :pyrometer-id :label]
                      "Default IR Pyrometer")
                     (:value @(rf/subscribe [::subs/get-field [:pyrometers]])))]
      [:div {:style {:flex 1}}
       (select-field "emissivity-type"
                     (translate
                      [:setting :tube-emissivity :label]
                      "Tube Emissivity")
                     [{:id "common" :name "Common for all tube"}
                      {:id "goldcup" :name "Gold Cup"}
                      {:id "custom" :name "Custom for each tube"}])]]

     [:div {:class "row"
            :style {:display "flex"}}
      [:div {:style {:flex 1}}
       [ui/raised-button
        {:label (translate [:setting :edit-pyrometer :label]
                           "Manage IR pyrometer")
         :label-position "after"
         :id "manage-pyrometer"
         :icon ""
         :on-click #(rf/dispatch [:tta.dialog.edit-pyrometer.event/open])}]]

      
      (let [emissivity-type (:value @(rf/subscribe [::subs/get-field [:emissivity-type]]))] 
     
        (case emissivity-type
          "custom" [:div {:style {:flex 1}}
                    [ui/raised-button
                     {:label (translate [:setting :tube-emissivity :label]
                                        "Tube Emissivity")
                      :label-position "after"
                      :id "tube-emissivity"
                      :icon ""
                      :on-click #(rf/dispatch
                                  [:tta.dialog.custom-emissivity.event/open])}]]

          "common" [:div {:style {:flex 1}}
                    (text-field "emissivity"
                                (translate [:setting :common-emissivity :label]
                                           "Emissivity")
                                "number"
                                {:required? true
                                 :number {:decimal false}})]
          nil))]
     [:div {:class "row"
            :style {:display "flex"}}
      [:div {:style {:flex 1}}
       (text-field "min-tubes%" (translate
                                [:setting :min-tubes%:label]
                                "Minimum TWT data needed to Publish data, %") "number"
                   {:number {:decimal? false
                             :min 20
                             :max 100}})]]]]])
