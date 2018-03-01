(ns tta.comp-set
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]))

(defonce my-state (r/atom {}))

(defn comp-set []
  (let [{:keys [check1 check2
                text1
                select1 select2]} @my-state
        disabled? (not check2)]
    [:div
     [:div {:style {:height "64px"}}
      [app-comp/toggle {:value check1
                        :on-toggle #(swap! my-state assoc :check1 %)}]
      [app-comp/toggle {:value check2
                        :on-toggle #(swap! my-state assoc :check2 %)
                        :disabled? (not check1)}]
      [app-comp/icon-button-s {:icon ic/plus}]
      [app-comp/icon-button-s {:icon ic/minus}]
      [app-comp/icon-button-s {:icon ic/plus, :disabled? true}]
      [app-comp/icon-button-s {:icon ic/minus, :disabled? true}]]
     [:div {:style {:height "64px"}}
      [app-comp/toggle {:value check1
                        :on-toggle #(swap! my-state assoc :check1 %)}]
      [app-comp/icon-button {:icon ic/plus
                             :on-click #(js/console.log "clicked")}]
      [app-comp/selector {:selected select1
                          :options ["Wall" "Burner" "Tube"]
                          :on-select #(swap! my-state assoc :select1 %1)
                          :item-width 70}]
      [app-comp/selector {:selected select2
                          :on-select #(swap! my-state assoc :select2 %1)
                          :options ["Data" "Graph"]
                          :disabled? disabled?
                          :item-width 70}]]
     [:div {:style {:height "64px"}}
      [app-comp/button {:on-click #(js/console.log "clicked!")
                        :disabled? disabled?
                        :icon ic/camera
                        :label "Screenshot"}]
      [app-comp/text-input {:read-only? false
                            :value text1
                            :on-change #(swap! my-state assoc :text1 %)
                            :align "center"
                            :width "72px"}]
      [app-comp/action-input-box {:disabled? disabled?
                                  :width "150px"
                                  :label "My data"
                                  :action #(js/console.log "clicked middle")
                                  :left-icon ic/nav-left
                                  :left-action #(js/console.log "clicked left")
                                  :left-disabled? true
                                  :right-icon ic/nav-right
                                  :right-action #(js/console.log "clicked right")}]
      [app-comp/dropdown-selector {:width "100px"
                                   :left-icon ic/pyrometer+
                                   :left-action #(js/console.log "manage list")
                                   :on-select #(swap! my-state assoc :selected %1)
                                   :selected (:selected @my-state)
                                   :items ["Option 1a sdlw asdi we t"
                                           "Option 2" "Option 3"]}]]
     [:div {:style {:height "64px"}}
      [app-comp/action-label-box {:width 150, :label "Tube row 1"
                                  :left-icon ic/nav-left
                                  :left-action #(js/console.log "clicked left")
                                  :left-disabled? disabled?
                                  :right-icon ic/delete
                                  :right-action #(js/console.log "clicked right")}]]]))
