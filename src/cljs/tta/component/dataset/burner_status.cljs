(ns tta.component.dataset.burner-status
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.style :refer [color color-hex color-rgba]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.view :as app-view]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :refer [lazy-cols table-grid]]
            [tta.component.dataset.burner-entry
             :refer [tf-burner-entry sf-burner-legend]]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]
            [tta.component.dataset.style :as style]))

;;;;side fired;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sf-burner [{:keys [row col ch-index]}]
  (let [dataset @(rf/subscribe [::subs/data])
        front @(rf/subscribe [::subs/burner-status-front-side ch-index])
        [b0 b1] (map #(get-in dataset [:side-fired :chambers ch-index
                                       :sides % :burners row col :state])
                     (range 2))
        bg-color (if (apply = (map #(= "on" %) [b0 b1]))
                   (color-hex :alumina-grey)
                   (color-hex :yellow))
        styles (style/sf-burner-status-styles bg-color)
        [bf bb] (map keyword (if (zero? front) [b0 b1] [b1 b0]))]
    [:div (use-style (:cell styles))
     [:div (use-style (get-in styles [:back bb]))]
     [:div (use-style (get-in styles [:front bf]))]]))

(defn sf-burner-table
  [width height row-count col-count dual-nozzle? ch-index burner-no]
  [table-grid
   {:height              height
    :width               width
    :row-header-width    64
    :col-header-height   30
    :row-count           (inc row-count)
    :col-count           (inc col-count)
    :row-height          64
    :col-width           64
    :labels              ["Row" "Burner"]
    :gutter              [10 10]
    :table-count         [1 1]
    :padding             [3 3 3 3]
    :row-header-renderer (fn [row [t-row t-col]]
                           [:div (use-sub-style style/sf-burner-table :row-head)
                            (if (< row row-count) (inc row))])
    :col-header-renderer (fn [col [t-row t-col]]
                           [:div (use-sub-style style/sf-burner-table :col-head)
                            (if (< col col-count) (burner-no col))])
    :cell-renderer       (fn [row col [t-row t-col]]
                           (if (and (< row row-count) (< col col-count))
                             [sf-burner {:row row, :col col
                                         :ch-index ch-index}]))}])

(defn sf-chamber-burner-status [_ _ ch-name side-names ch-index dual-nozzle?
                                burner-row-count burner-count-per-row burner-no]
  (fn [width height _ _ _ _ _ _ _]
    (let [col-width 64
          w (- width 40)
          w2 (* (inc burner-count-per-row) col-width)
          w2 (if (> w2 w) w w2)
          h1 60
          h2 (- height (+ h1 48 40))
          front-side @(rf/subscribe [::subs/burner-status-front-side ch-index])]
      [:div (update (use-sub-style style/sf-burner-table :body) :style
                    assoc :height height, :width width)
       [:div {:style {:height h1}}
        [:div (update (use-sub-style style/sf-burner-table :title) :style
                      assoc :width w)
         ch-name]
        [:div {:style {:position "absolute", :top 10, :left 20}}
         [:div (use-sub-style style/sf-burner-table :label)
          (translate [:dataset :select-front-side :label] "Choose side on front")]
         [app-comp/selector
          {:item-width 100
           :options [0 1]
           :selected front-side
           :label-fn #(get side-names %)
           :on-select #(rf/dispatch [::event/set-burner-status-front-side ch-index %])}]]]
       [:div {:style {:height h2, :width w}}
        [:div {:style {:width w2, :margin "auto", :color (color-hex :royal-blue)}}
         [sf-burner-table w2 h2
          burner-row-count burner-count-per-row dual-nozzle? ch-index burner-no]]]
       [sf-burner-legend dual-nozzle?]])))

(defn sf-burner-status [width height]
  (let [config @(rf/subscribe [::subs/config])
        ch-count (count (get-in config [:sf-config :chambers]))
        dual-nozzle? (get-in config [:sf-config :dual-nozzle?])
        render-fn (fn [indexes _]
                    (map (fn [ch-index]
                           (let [{:keys [burner-row-count burner-count-per-row
                                         start-burner end-burner name side-names]}
                                 (get-in config [:sf-config :chambers ch-index])
                                 burner-no #(if (> end-burner start-burner)
                                              (+ start-burner %)
                                              (- start-burner %))]
                             [sf-chamber-burner-status (- width 20) (- height 30)
                              name side-names
                              ch-index dual-nozzle?
                              burner-row-count burner-count-per-row burner-no]))
                         indexes))]
    [lazy-cols {:width width, :height height
                :item-width width
                :item-count ch-count
                :items-render-fn render-fn}]))
;;;top-fired;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tf-burner-status [width height]
  [tf-burner-entry width height])

;;;body;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn burner-status [{{:keys [width height]} :view-size}]
  (if-let [firing @(rf/subscribe [::subs/firing])]
    (case firing
      "side" [sf-burner-status width height]
      "top" [tf-burner-status width height])))
