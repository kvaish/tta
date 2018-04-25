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
            [tta.component.dataset.event :as event]))
;;;;side fired;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def sf-burner-style
  (let [col-on      (color-hex :green)
        col-off     (color-hex :red)
        col-off-aux (color-hex :orange)
        col-off-fix (color-hex :brown)
        circle      {:width         "24px", :height "24px"
                     :position      "absolute"
                     :top           "24px", :left   "24px"
                     :border-radius "50%"
                     :background    (color-hex :white)
                     :box-shadow    "inset -6px -6px 10px rgba(0,0,0,0.3)"}
        side-circle (assoc circle :left "14px" :top "15px")]
    
    {:height   "48px", :width "48px"
     :display  "inline-block"
     :position "relative"
     ::stylefy/sub-styles
     {:side-on      (assoc side-circle  :background col-on)
      :side-off     (assoc side-circle  :background col-off)
      :side-off-aux (assoc side-circle :background col-off-aux)
      :side-off-fix (assoc side-circle :background col-off-fix)
      :on           (assoc circle :background col-on)
      :off          (assoc circle :background col-off)
      :off-aux      (assoc circle :background col-off-aux)
      :off-fix      (assoc circle :background col-off-fix)
      :circle       (assoc circle
                           :top "36px", :left "36px"
                           :box-shadow "-3px -3px 10px 3px rgba(0,0,0,0.3)")}}))
(defn sf-burner-value-fn [ch-index row col side-names]
  (let [dataset @(rf/subscribe [::subs/data])]
    
    {(first side-names) (get-in dataset [:side-fired :chambers ch-index :sides
                                         0 :burners row col :state])
     (second side-names) (get-in dataset [:side-fired :chambers ch-index  :sides
                                          1 :burners row col :state])}))

(defn sf-burner [{:keys [row col ch-index] :as props}]
  (let [side-names
        (get-in @(rf/subscribe [::subs/config])
                [:sf-config :chambers ch-index :side-names]) 
        style sf-burner-style]
    (fn [props]
      (let [sel-side (or @(rf/subscribe [::subs/burner-status-active-side ch-index])
                         (first side-names))
            value (sf-burner-value-fn ch-index row col side-names)
            sel-side-val (get value sel-side)
            other-val (first (map val (dissoc value sel-side)))
            bg-color (if-not (= sel-side-val other-val)
                       (color-hex :yellow)
                       (color-hex :alumina-grey))
            circle-border {:style {:border (str "1px solid" bg-color)}}]
        
        [:div {:style {:width            64 :height 64
                       :background-color bg-color
                       :border           (str "1px solid" (color-hex :white))}}
         [:div  (merge (stylefy/use-sub-style style 
                                              (keyword (str "side-"(or other-val  "off"))))
                       circle-border)]
         [:div  (merge (stylefy/use-sub-style style
                                              (keyword (or sel-side-val "off")))
                       circle-border)]])))) 

(defn sf-burner-table
  [width height row-count col-count dual-nozzle? ch-index]
  [table-grid
   {:height              height
    :width               width
    :row-header-width    64
    :col-header-height   30
    :row-count           row-count
    :col-count           col-count
    :row-height          64
    :col-width           64
    :labels              ["Row" "Burner"]
    :gutter              [20 20]
    :table-count         [1 1]
    :padding             [3 3 15 15]
    :row-header-renderer (fn [row [t-row t-col]]
                           [:div {:style {:text-align   "center"
                                          :line-height  "64px"
                                          :border-right "1px solid"
                                          :border-color (color-hex :sky-blue)
                                          :height       "inherit"}}
                            (inc row)])
    :col-header-renderer (fn [col [t-row t-col]]
                           [:div {:style {:text-align    "center"
                                          :border-bottom "1px solid"
                                          :border-color  (color-hex :sky-blue)
                                          :height        "inherit"}}
                            (inc col)])
    :cell-renderer       (fn [row col [t-row t-col]]
                           [sf-burner
                            {:row          row
                             :ch-index     ch-index
                             :col          col}])}])

(defn sf-chamber-burner-status [width height ch-name side-names ch-index
                                dual-nozzle? burner-row-count burner-count-per-row]
  (let [col-width 64
        width (- width 20)
        w (* burner-count-per-row col-width)
        w (if (> (+ w 20) width) width w)
        h1          100
        h3          50
        h2          (- height (+ h1 h3 40))]
    (fn [width height ch-name side-names] 
      (let [active-side (or @(rf/subscribe [::subs/burner-status-active-side ch-index])
                            (first side-names))]
        [:div {:style {:height        height
                       :width         width
                       :border        (str "1px solid" (color-hex :sky-blue))
                       :border-radius "6px"
                       :padding "10px"}}

         [:div {:style {:height h1 :width width}}
          [:div {:style {:width      width
                         :text-align "center"}} ch-name]
          [:div {:style {:font-size "14px"
                         :color     (color-hex :sky-blue)}}
           [:div  {:style {:display    "inline-block"
                           :margin-top "14px"}}
            (translate [::dataset :side-on-front :label] "side on front")]
           [:div {:style {:display        "inline-block"
                          :vertical-align "top"}}
            [app-comp/selector
             {:item-width 150
              :options    side-names
              :selected   active-side
              :on-select  #(rf/dispatch [::event/set-burner-status-active-side
                                         ch-index %])}]]] ]

         [:div {:style {:height h2 :width width}}
          [:div {:style {:width w :margin "auto"}}
           [sf-burner-table (- width 40) (- h2 10)
            burner-row-count burner-count-per-row
            dual-nozzle? ch-index]]]

         
         [:div {:style {:height h3 :width width :text-align  "center"}}
          [sf-burner-legend dual-nozzle?]]
         ]))))

(defn sf-burner-status [width height]
  (let [config @(rf/subscribe [::subs/config])
        ch-count (count (get-in config [:sf-config :chambers]))
        dual-nozzle? (get-in config [:sf-config :dual-nozzle?])
        render-fn (fn [indexes _]
                    (map (fn [ch-index]
                           (let [{:keys [burner-row-count burner-count-per-row]}
                                 (get-in config [:sf-config :chambers ch-index])]
                             [sf-chamber-burner-status (- width 20) (- height 30)
                              (get-in config [:sf-config :chambers ch-index :name])
                              (get-in config [:sf-config :chambers ch-index :side-names])
                              ch-index dual-nozzle?
                              burner-row-count burner-count-per-row]))
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
