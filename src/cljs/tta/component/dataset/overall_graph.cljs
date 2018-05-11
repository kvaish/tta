(ns tta.component.dataset.overall-graph
  (:require [reagent.core :as r]
            [reagent.format :refer [format]]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.util.common :as u]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.comp :as app-comp]
            [tta.app.view :as app-view]
            [tta.app.charts :refer [overall-twt-chart]]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]))

(defn graph-body [style level-key title sub-title]
  (let [{:keys [w h2]} (:data (meta style))
        ch-data @(rf/subscribe [::subs/overall-twt-graph-data level-key])
        props (assoc ch-data
                     :height h2, :width w
                     :title title, :sub-title sub-title)
        options {:reduced-firing? @(rf/subscribe [::subs/reduced-firing?])}]
    [:div (use-sub-style style :body)
     [overall-twt-chart props options]]))

(defn graph-header [style level-key title sub-title]
  [:div (use-sub-style style :header)
   [:div (use-sub-style style :title) title]
   [:div (use-sub-style style :sub-title) sub-title]])

(defn graph-footer [style level-key]
  [:div (use-sub-style style :footer)
   [:span
    [:span (use-sub-style style :label)
     (translate [:overall-chart :legend :reduced-firing] "Reduced firing")]
    [app-comp/toggle
     {:value @(rf/subscribe [::subs/reduced-firing?])
      :on-toggle #(rf/dispatch [::event/set-reduced-firing? %])}]]])

(defn overall-graph [{:keys [level]
                      {:keys [width height]} :view-size}]
  (let [style (style/overall-twt-graph width height)
        level (get @(rf/subscribe [::subs/level-opts]) level)
        {:keys [label], level-key :id} level
        title (str (translate [:dataset :overall-chart :title]
                              "Overall tube wall temperature")
                   ", " label)
        data-date @(rf/subscribe [::subs/data-date])
        {:keys [year month day hour minute]} (u/to-date-time-map data-date)
        sub-title (format "%4d-%02d-%02d | %02d:%02d" year month day hour minute)]
    [:div (use-style style)
     [graph-header style level-key title sub-title]
     [graph-body style level-key title sub-title]
     [graph-footer style level-key]]))
