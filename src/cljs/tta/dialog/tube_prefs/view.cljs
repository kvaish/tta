;; view elements dialog tube-prefs
(ns tta.dialog.tube-prefs.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [ht.util.interop :as i]
            [tta.dialog.tube-prefs.subs :as subs]
            [tta.dialog.tube-prefs.event :as event]
            [ht.app.comp :as ht-comp]
            [tta.app.comp :as app-comp]
            [tta.app.icon :as ic]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.input :refer [list-tube-prefs]]))

(def container (r/atom {}))
(defn- tube-prefs-component [tube-prefs]
  (r/create-class
   {:component-did-mount 
    (fn [this]
      (swap! container  assoc
             :width (i/oget-in this [:refs :container :offsetWidth])
             :height 400))
    :reagent-render
    (fn [tube-prefs]
      [:div {:ref "container"}  
       (let [height 400
             plant @(rf/subscribe [::app-subs/plant])
             items-render-fn
             (fn [indexes show-item]
               (map (fn [i]
                      (let [
                            {:keys [start-tube end-tube tube-count]}
                            (or
                             (get-in plant [:config :sf-config :chambers i])
                             (get-in plant [:config :tf-config :tube-rows i]))
                            label (case (get-in plant [:config :firing])
                                    "side" "Chamber"
                                    "top" "Row")]
                        [list-tube-prefs {:label (str label " " (inc i)) 
                                          :height height
                                          :start-tube start-tube
                                          :end-tube end-tube  
                                          :on-clear #(rf/dispatch [::event/clear-tube-prefs i]) 
                                          :selected-fn (fn [%1 %2] @(rf/subscribe
                                                                    [::subs/field
                                                                     (conj [] i %1)]))
                                          :on-select #(rf/dispatch
                                                       [::event/set-field
                                                        i %1 %2])}]))
                    indexes))]
         
         [lazy-cols {:height height
                     :width (:width @container)
                     :item-width 310
                     :item-count (count tube-prefs)
                     :items-render-fn items-render-fn}])])}))
(defn tube-prefs []
  (let [open? @(rf/subscribe [::subs/open?])
        tube-prefs @(rf/subscribe [::subs/data])
        title (translate [:tube-preference :dialog :title] "Tube Preference")
        close-tooltip (translate [:app :tooltip :close] "Close")
        on-close #(rf/dispatch [::event/close])] 
    
    [ui/dialog
     {:open open?
      :modal true
      :title (r/as-element (ht-comp/optional-dialog-head
                            {:title title
                             :on-close on-close
                             :close-tooltip close-tooltip})) 

      :actions (r/as-element
                [:div
                 [app-comp/button {:icon ic/cancel
                                   :label (translate [:action :cancel :label] "Cancel")
                                   :on-click #(rf/dispatch [::event/close])}]
                 [app-comp/button {:disabled? (not @(rf/subscribe [::subs/can-submit?]))
                                   :icon ic/accept
                                   :label (translate [:action :done :label] "Done")
                                   :on-click #(rf/dispatch [::event/submit])}]])}
     [tube-prefs-component tube-prefs]]))
