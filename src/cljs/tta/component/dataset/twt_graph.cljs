(ns  tta.component.dataset.twt-graph
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [reagent.format :refer [format]]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.view :as app-view]
            [ht.util.common :as u]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.charts :refer [twt-chart]]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]))

(defn twt-graph-row [props level-key row-index]
  (let [ch-data @(rf/subscribe [::subs/twt-graph-row-data level-key row-index])
        props (merge ch-data props)]
    [twt-chart props]))

(defn twt-graph-body [style level-key title sub-title]
  (let [{:keys [w h2]}  (:data (meta style))
        item-width 800
        chart-props {:height (- h2 20)
                     :width (- item-width 20)
                     :title title
                     :sub-title sub-title}
        {:keys [firing tf-config sf-config]} @(rf/subscribe [::subs/config])
        item-count (if (= "top" firing)
                     (:tube-row-count tf-config)
                     (count (:chambers sf-config)))
        items-render-fn
        (fn [indexes _]
          (map (fn [i]
                 [twt-graph-row chart-props level-key i])
               indexes))]
    [:div (use-sub-style style :body)
     [lazy-cols {:width w, :height h2
                 :item-width item-width
                 :item-count item-count
                 :items-render-fn items-render-fn}]]))

;; HEADER ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn twt-graph-header [style level-key title sub-title]
  [:div (use-sub-style style :header)
   [:span (use-sub-style style :label)
    (translate [:dataset :choose-twt-type :label] "Choose TWT type")]
   (let [opts @(rf/subscribe [::subs/twt-type-opts])
         sel @(rf/subscribe [::subs/twt-type])
         sel (some #(if (= (:id %) sel) %) opts)]
     [app-comp/selector {:item-width 80
                         :options opts
                         :selected sel
                         :label-fn :label
                         :value-fn :id
                         :on-select #(rf/dispatch [::event/set-twt-type (% :id)])}])
   [:div (use-sub-style style :title) title]
   [:div (use-sub-style style :sub-title) sub-title]])

;; FOOTER ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn twt-graph-footer [style level-key]
  [:div (use-sub-style style :footer)
   ;; toggle-reduced-firing
   [:span
    [:span (use-sub-style style :label)
     (translate [:twt-chart :legend :reduced-firing] "Reduced firing")]
    [app-comp/toggle
     {:value  @(rf/subscribe [::subs/reduced-firing?])
      :on-toggle #(rf/dispatch [::event/set-reduced-firing? %])}]]
   ;; toggle-avg-temp-band
   [:span {:style {:margin-left "20px"}}
    [:span (use-sub-style style :label)
     (let [delta @(rf/subscribe [::subs/avg-temp-band])]
       (translate [:twt-chart :legend :avg-temp-band]
                  "{delta} off Avg temp"
                  {:delta (str "±" delta)}))]
    [app-comp/toggle
     {:value  @(rf/subscribe [::subs/avg-temp-band?])
      :on-toggle #(rf/dispatch [::event/set-avg-temp-band? %])}]]
   ;; toggle-avg-corrected-temp
   [:span {:style {:margin-left "20px"}}
    [:span (use-sub-style style :label)
     (translate [:twt-chart :legend :avg-raw-temp] "Avg raw temp")]
    [app-comp/toggle
     {:value @(rf/subscribe [::subs/avg-raw-temp?])
      :on-toggle #(rf/dispatch [::event/set-avg-raw-temp? %])}]]
   ;; toggle-avg-raw-temp
   [:span {:style {:margin-left "20px"}}
    [:span (use-sub-style style :label)
     (translate [:twt-chart :legend :avg-temp] "Avg temp")]
    [app-comp/toggle
     {:value @(rf/subscribe [::subs/avg-temp?])
      :on-toggle #(rf/dispatch [::event/set-avg-temp? %])}]]])

;; TWT-GRAPH ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn twt-graph [{:keys [level]
                  {:keys [width height]} :view-size}]
  (let [style (style/twt-graph width height)
        firing @(rf/subscribe [::subs/firing])
        level-key @(rf/subscribe [::subs/level-key level])
        level (get @(rf/subscribe [::subs/level-opts]) level)
        title (str (translate [:dataset :twt-chart :title] "Tube wall temperature")
                   (if (= "top" firing)
                     (str ", " (:label level))))
        data-date @(rf/subscribe [::subs/data-date])
        {:keys [year month day hour minute]} (u/to-date-time-map data-date)
        sub-title (format "%4d-%02d-%02d | %02d:%02d" year month day hour minute)]
    [:div (use-style style)
     (twt-graph-header style level-key title sub-title)
     (twt-graph-body style level-key title sub-title)
     (twt-graph-footer style level-key)]))
