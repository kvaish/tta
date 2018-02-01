;; view elements component setting
(ns tta.component.setting.view
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
            [tta.component.setting.style :as style]
            [tta.component.setting.subs :as subs]
            [tta.component.setting.event :as event]))


(defn setting [props]
  (rf/dispatch [::event/init-setting-comp])
  [:div (use-style style/setting)
   [:div {:class "sub-header"}
    [ui/toolbar
     (merge 
      {:style style/content-toolbar
       :class-name "test-toolbar"})
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
        [ui/select-field
         {:floating-label-text "Temperature Unit"
          :floating-label-fixed false
          :value  @(rf/subscribe [::subs/get-field [:temp-unit]])
          :on-change #(rf/dispatch
                       [::event/set-field [:temp-unit] %3])}
         
         (map (fn [{:keys [key value]}]
                [ui/menu-item {:key key
                               :value value
                               :primary-text key
                               :on-change #(rf/dispatch
                                            [::event/set-field [:temp-unit] %3])}])
              [{:key "°C" :value "°C" }
               {:key "°F" :value "°F"}])
         ])]
     [:div {:class "row"}
      [ui/text-field
       {:on-change #(rf/dispatch [::event/set-field [:design-temp]])
        :default-value @(rf/subscribe [::subs/get-field [:design-temp]])
        :floating-label-text (translate
                              [:setting :design-temp :label]
                              "Design Temperature")
        :name "design-temp"
        :hint-text "Design Temperature"
        :id "design-temp"
        :type "number"
        }]
      [ui/text-field
       {:on-change #(rf/dispatch [::event/set-field [:target-temp]])
        :default-value @(rf/subscribe [::subs/get-field [:target-temp]])
        :floating-label-text (translate
                              [:setting :target-temp :label]
                              "Target Temperature")
        :name "target-temp"
        :hint-text "Target Temperature"
        :id "target-temp"
        :type "number"
        }]

      ]
     
     ]
    ]])

(defn text-field [name sub change label]
  [ui/text-field
   {:on-change #(rf/dispatch [::event/set-field [:design-temp]])
    :default-value @(rf/subscribe [::subs/get-field [:design-temp]])
    :floating-label-text (translate
                          [:setting :design-temp :label]
                          "Design Temperature")
    :name "design-temp"
    :hint-text "Design Temperature"
    :id "design-temp"
    :type "number"
    }])
