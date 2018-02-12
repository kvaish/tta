

;; view elements dialog custom-emissivity
(ns tta.dialog.custom-emissivity.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.view :as app-view]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.dialog.custom-emissivity.style :as style]
            [tta.dialog.custom-emissivity.subs :as subs]
            [tta.dialog.custom-emissivity.event :as event]
            [tta.component.settings.subs :as setting-subs]))

(defn- text-field [id label type pyrometer-id validations]
  (let [field-path (conj [] (keyword id))
        field @(rf/subscribe [::subs/get-pyrometer-field field-path pyrometer-id])]
    [ui/text-field 
     {:on-change #(rf/dispatch [::event/set-field field-path %2 pyrometer-id  validations])
      :default-value (:value field)
      :hint-text label
      :error-text (:error field)
      :floating-label-fixed true
      :style {:width "150px"
              :margin "5px"}
      :name id
      :type type
      :label label}]))

(defn custom-emissivity []
  (let [open? @(rf/subscribe [::subs/open?])
        title (translate [:custom-emissivity :dialog :title]
                         "Custom Emissivity for each tube")
        close-tooltip "close"
        on-close #(rf/dispatch [::event/discard-data])
        data @(rf/subscribe [::subs/data-custom-emissivity])
        config-data @(rf/subscribe [::subs/config-data])
        sub-fillall @(rf/subscribe [::subs/field "fill-all"])]
    
    [ui/dialog
     {:open open?
      :modal true
      :auto-scroll-body-content false
      :title (r/as-element (app-view/optional-dialog-head
                            {:title title
                             :on-close on-close
                             :close-tooltip close-tooltip}))
      :actions (r/as-element
                [:div {:class "fill-all"
                       :style {:display "inline-block"}}
                 
                 [ui/text-field
                  {:label (translate
                           [:custom-emissivity :fill-all :label] "Fill all")
                   
                   :floating-label-fixed true
                   :on-change #(rf/dispatch [::event/set-field
                                             "fill-all" %2
                                             {:number {:min 0.1
                                                       :max 0.99}}])

                   :error-text (:error sub-fillall)
                   :floating-label-text (translate
                                         [:custom-emissivity :fill-all :label] "Fill all") 
                   :hint (translate
                          [:custom-emissivity :fill-all :label] "Fill all")
                   :type "number"
                   :style {:width "100px"
                           :margin-right "20px"}}]

                 [ui/raised-button
                  {:label (translate
                           
                           [:custom-emissivity :fill-all :button] "Fill")
                   :on-click #(rf/dispatch [::event/fill-all (:value sub-fillall)])
                   :disabled (not (:valid? sub-fillall))
                   :style {:height "30px"}}]

                 [ui/raised-button
                  {:label (translate
                           [:custom-emissivity :continue :button] "Continue")
                   :disabled (not @(rf/subscribe [::subs/valid-dirty?]))
                   :on-click #(rf/dispatch [::event/save-data])
                   :style {:height "30px"
                           :margin-left "10px"}}]
                 ])}
     
     [:div {:style {:display "flex"
                    :height "100%"
                    :width "100%"}}
      
      (doall (map-indexed (fn [idx {:keys [name start-tube end-tube side-names]}]
                            [:div {:class "row"
                                   :key (str name idx)
                                   :style {:flex 1
                                           :height "100%"
                                           :max-height "700px"
                                           :width "100%"
                                           :overflow-y "scroll"}
                                   }
                             [:div {:style {:display :flex
                                            :text-align "center"}}
                              [:h5 {:style {:flex 1
                                            }}(last side-names)]

                              [:h5 {:style {:flex 1
                                            }} name]

                              [:h5 {:style {:flex 1
                                            }} (last side-names)]
                              ]
                             [:div {:style {:width "100%"
                                            :text-align "center"}
                                    }
                              (doall
                               (map (fn [m]
                                      (let [val @(rf/subscribe
                                                  [::subs/custom-emissivity-field
                                                   idx 
                                                   0
                                                   (dec m)])]
                                        [:div 
                                         [ui/text-field
                                          {:key (str name  m (first side-names))
                                           :name (str name  m (first side-names))
                                           :default-value (:value val)
                                           :error-text (:error  @(rf/subscribe
                                                                  [::subs/custom-emissivity-field
                                                                   idx
                                                                   0
                                                                   (dec m)]))
                                           :type "number"
                                           :on-change #(rf/dispatch
                                                        [::event/set-emissivity-field {:chamber idx
                                                                                       :side 0
                                                                                       :tube (dec m)
                                                                                       :value %2}{:required true
                                                                                       :number {:min 0
                                                                                                :max 0.99}}])
                                           :style {:width "50px"
                                                   :margin "15px"}}]
                                               
                                         [:div (merge (use-style style/tube)
                                                      {:key m})
                                          m]
                                         [ui/text-field
                                          {:key (str name  m (last side-names))
                                           :name (str name  m (first side-names))
                                           :default-value (:value @(rf/subscribe
                                                                    [::subs/custom-emissivity-field
                                                                     idx 1  (dec m)]))
                                           :error-text (:error  @(rf/subscribe
                                                                  [::subs/custom-emissivity-field
                                                                   idx
                                                                   1
                                                                   (dec m)]))
                                           :type "number"
                                           :on-change #(rf/dispatch
                                                        [::event/set-emissivity-field {:chamber idx
                                                                            :side 1
                                                                            :tube (dec m)
                                                                            :value %2} {:required true
                                                                            :number {:min 0
                                                                                     :max 0.99}}])
                                           :style {:width "50px"
                                                   :margin "15px"}}]
                                         ]) )
                                    (range start-tube (+ 1 end-tube))))
                              ]]             
                            )config-data))]]))
