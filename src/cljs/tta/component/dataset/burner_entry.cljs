(ns tta.component.dataset.burner-entry
  (:require [reagent.core :as r]
            [reagent.validation :as rv]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.style :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :as scroll :refer [lazy-cols]]
            [tta.app.view :as app-view]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]))

;; TOP FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def tf-burner-style
  (let [circle {:width "32px", :height "32px"
                :display "inline-block"
                :border-radius "50%"
                :font-size "10px"
                :line-height "32px"
                :text-align "center"
                :vertical-align "top"
                :margin-top "8px"
                :color (color-hex :white)}]
    {:height   "48px", :width "120px"
     ::stylefy/sub-styles
     {:circle       circle
      :palette      {:width "48px"}
      :palette-unit {:display "inline-block"
                     :width "24px", :height "50px"}
      :temp-scale   {:vertical-align "top"
                     :margin-left "5px", :font-size "12px"}}}))
(def opening-color-map
  {15 (color-hex :red)
   30 (color-hex :orange)
   45 (color-hex :green)
   60 (color-hex :teal)
   75 (color-hex :indigo)
   90 (color-hex :royal-blue)})

(defn opening->color [opening]
  (if (and opening (not= opening ""))
    (some (fn [i]
            (if (<= opening i)
              (get opening-color-map i)))
          (keys opening-color-map))
    (color-hex :slate-grey)))

(defn color-palette []
  (let [style tf-burner-style
        distribution [90 75 60 45 30 15]]
    [:div (stylefy/use-sub-style style :palette)
     (doall
      (map (fn [i] ^{:key i}
             [:div {:style {:height "50px"}}
              [:div (merge (stylefy/use-sub-style style :palette-unit)
                           {:style {:background (opening->color i)}})]
              [:span (stylefy/use-sub-style style :temp-scale) (str i "°")]])
           distribution))
     [:span (merge (stylefy/use-sub-style style :temp-scale)
                   {:style {:float "right"
                            :padding-right "6px"
                            :margin-top "-7px"}}) "0°"]]))

(defn tf-burner-parse [value]
  (if (and (rv/valid-number? value) (not-empty value))
    (let [v (js/Number value)]
      (if (and (<= 0 v 90)
               (= v (js/Math.ceil v)))
        v))))

(defn tf-burner [{:keys [value-fn label-fn on-change row col]}]
  (let [style tf-burner-style
        value-fn #(value-fn row col)
        label (label-fn row col)
        on-change #(if-let [v (tf-burner-parse %)]
                     (on-change row col v)
                     (if (empty? %)
                       (on-change row col nil)))]
    (fn [_]
      (let [value (value-fn)]
        [:div (stylefy/use-style style)
         [:div (merge (stylefy/use-sub-style style :circle)
                      {:style {:background (opening->color value)}})
          label]
         [app-comp/text-input
          {:width     48
           :value     value
           :on-change on-change}]]))))

(defn tf-burner-table
  "TODO:
  **value-fn**
  **label-fn**
  **on-change**"
  [width height
   burner-row-count burner-count-per-row
   value-fn label-fn on-change
   wall-labels]
  (let [w2 200
        w1 (- width w2)
        h1 60
        h2 (- height h1 h1)]
    [:div {:style {:height    height
                   :width     width
                   :font-size "12px"}}
     [:div {:style {:width          w1, :height height
                    :display        "inline-block"
                    :vertical-align "top"}}
      [:div {:style {:width       w1
                     :height      h1
                     :padding-top "25px"}}
       [:div {:style {:color      (color-hex :royal-blue)
                      :text-align "center"}}
        (:north wall-labels)]
       [:div {:style {:font-size "10px"
                      :color     (color-hex :sky-blue)}}
        (translate [:data-entry :burner-row :label]
                   "Burner Row")]]
      [:div {:style {:width w1, :height h2}}
       (let [col-width 120
             w (* col-width burner-row-count)
             w (if (> (+ w 20)  w1) w1 w)]
         [:div {:style {:width w :margin "auto"}}
          [scroll/table-grid
           {:height              h2
            :width               w
            :row-header-width    0
            :col-header-height   30
            :row-count           burner-count-per-row
            :col-count           burner-row-count
            :row-height          48
            :col-width           120
            :table-count         [1 1]
            :padding             [1 1 1 1]
            :row-header-renderer (fn [row [t-row t-col]]
                                   nil)
            :col-header-renderer (fn [col [t-row t-col]]
                                   [:div {:style {:text-align "center"}}
                                    (inc col)])
            :cell-renderer       (fn [row col [t-row t-col]]
                                   [tf-burner {:value-fn  value-fn
                                               :label-fn  label-fn
                                               :on-change on-change
                                               :row row, :col col}])}]])]
      [:div {:style {:width w1, :height h1}}
       [:div {:style {:color      (color-hex :royal-blue)
                      :text-align "center"}}
        (:south wall-labels)]]]
     [:div {:style {:width          w2, :height height
                    :display        "inline-block"
                    :vertical-align "top"
                    :padding        "60px 0 0 100px"}}
      [color-palette]]]))


;; SIDE FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def sf-burner-style
  (let [col-on (color-hex :green)
        col-off (color-hex :red)
        col-off-aux (color-hex :orange)
        col-off-fix (color-hex :brown)
        circle {:width "24px", :height "24px"
                :position "absolute"
                :top "24px", :left "24px"
                :border-radius "50%"
                :background (color-hex :white)
                :box-shadow "inset -6px -6px 10px rgba(0,0,0,0.3)"}]
    {:height "48px", :width "48px"
     :display "inline-block"
     :position "relative"
     ::stylefy/sub-styles
     {:on (assoc circle :background col-on)
      :off (assoc circle :background col-off)
      :off-aux (assoc circle :background col-off-aux)
      :off-fix (assoc circle :background col-off-fix)
      :popup {:position "relative"
              :height "96px", :width "96px"
              :top "-12px", :left "-12px"
              :z-index "99999"
              :border-radius "50%"
              :background (color-rgba :white 0 0.5)
              :box-shadow "0 0 10px 3px rgba(0,0,0,0.3),
inset -3px -3px 10px rgba(0,0,0,0.3)"}
      :circle (assoc circle
                     :top "36px", :left "36px"
                     :box-shadow "-3px -3px 10px 3px rgba(0,0,0,0.3)")}}))

(defn sf-burner-popup [{:keys [state]}]
  (let [style sf-burner-style
        {:keys [value on-change hover? dual-nozzle?]} @state]
    (if hover?
      (into [:div (stylefy/use-sub-style style :popup)
             [:div (stylefy/use-sub-style style :circle)]
             [:div (update (stylefy/use-sub-style style (keyword (or value "off")))
                           :style assoc
                           :top "6px", :left "36px")]]
            (->> (if dual-nozzle?
                   [[:on "42px" "6px"]
                    [:off "42px" "66px"]
                    [:off-fix "64px" "22px"]
                    [:off-aux "64px" "50px"]]
                   [[:on "48px" "8px"]
                    [:off "48px" "64px"]
                    [:off-fix "66px" "36px"]])
                 (map (fn [[k t l]]
                        [:div (-> (stylefy/use-sub-style style k)
                                  (update :style assoc :top t, :left l
                                          :cursor "pointer")
                                  (assoc :on-click #(let [value (name k)]
                                                      (swap! state assoc :value value)
                                                      (on-change value))))])))))))

;; 48x48
(defn sf-burner [{:keys [value-fn on-change row col side] :as props}]
  (let [style sf-burner-style
        value-fn (partial value-fn side row col)
        on-change (partial on-change side row col)
        state (r/atom (assoc props :value-fn value-fn, :on-change on-change))
        mouse-enter #(swap! state assoc :hover? true)
        mouse-leave #(swap! state assoc :hover? false)]
    (fn [props]
      (let [value (value-fn)]
        (swap! state assoc :value value)
        [:div (merge (stylefy/use-style style)
                     {:on-mouse-enter mouse-enter
                      :on-mouse-leave mouse-leave})
         [:div (stylefy/use-sub-style style (keyword (or value "off")))]
         [sf-burner-popup {:state state}]]))))

(defn sf-burner-table
  "TODO:
  **value-fn**
  **on-change**"
  [width height row-count col-count value-fn on-change dual-nozzle?]
  [scroll/table-grid
   {:height              height :width width
    :row-header-width    64 :col-header-height 30
    :row-count           row-count :col-count col-count
    :row-height          64 :col-width 64
    :labels              ["Row" "Burner"]
    :gutter              [20 20]
    :table-count         [1 2]
    :padding             [3 3 15 15]
    :row-header-renderer (fn [row [t-row t-col]]
                           [:div {:style {:text-align "center"
                                          :line-height "64px"
                                          :border-right "1px solid"
                                          :border-color (color-hex :sky-blue)
                                          :height "inherit"}}
                            (inc row)])
    :col-header-renderer (fn [col [t-row t-col]]
                           [:div {:style {:text-align "center"
                                          :border-bottom "1px solid"
                                          :border-color (color-hex :sky-blue)
                                          :height "inherit"}}
                            (inc col)])
    :cell-renderer       (fn [row col [t-row t-col]]
                           [sf-burner {:value-fn     value-fn
                                       :on-change    on-change
                                       :dual-nozzle? dual-nozzle?
                                       :row row
                                       :col col
                                       :side t-col}])}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tf-burner-label [burner-row index]
  (let [{:keys [start-burner end-burner]} burner-row]
    (if (> end-burner start-burner)
      (+ start-burner index)
      (- start-burner index))))

(defn tf-burner-entry [width height]
  (let [{:keys [burner-row-count burner-rows wall-names]}
        (:tf-config @(rf/subscribe [::subs/config]))
        burner-count (get-in burner-rows [0 :burner-count])
        value-fn #(deref (rf/subscribe [::subs/tf-burner [%2 %1]]))
        label-fn #(tf-burner-label (get burner-rows %2) %1)
        on-change #(rf/dispatch [::event/set-tf-burner [%2 %1] %3])]
    (fn [_ _]
      [tf-burner-table
       width height
       burner-row-count burner-count
       value-fn label-fn on-change
       wall-names])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sf-burner-legend [dual-nozzle?]
  (let [style sf-burner-style]
    [:div {:style {:height 48
                   :font-size "12px"}}
     (->> [[:on (translate [:dataset :burner :on] "On")]
           [:off (translate [:dataset :burner :off] "Off")]
           [:off-fix (translate [:dataset :burner :off-fix]
                                "Off for maintenance")]
           (if dual-nozzle?
             [:off-aux (translate [:dataset :burner :off-aux]
                                  "Secondary fuel off")])]
          (remove nil?)
          (map (fn [[b label]]
                 [:div {:style {:position "relative"
                                :height 48
                                :display "inline-block"}}
                  [:div (stylefy/use-sub-style style b)]
                  [:span {:style {:margin "30px 0 0 60px"
                                  :display "inline-block"}} label]]))
          (into [:div {:style {:width 600, :margin "auto"}}]))]))

(defn sf-chamber-burner-entry [_ _ ch-name side-names ch-index dual-nozzle?
                               row-count col-count]
  (let [value-fn (fn [side row col]
                   @(rf/subscribe [::subs/sf-burner [ch-index side row col]]))
        on-change (fn [side row col value]
                    (rf/dispatch [::event/set-sf-burner
                                  [ch-index side row col] value]))]
    (fn [width height _ _]
      [:div {:style {:width width, :height height
                     :border (str "1px solid " (color-hex :sky-blue))
                     :border-radius "6px"
                     :padding "20px"}}
       [:div {:style {:width (- width 40), :height 60
                      :text-align "center"
                      :font-size "14px"
                      :color (color-hex :royal-blue)}}
        ch-name
        (into [:div]
              (map-indexed (fn [i side-name]
                             [:div {:key i
                                    :style {:width (/ (- width 40) 2)
                                            :display "inline-block"}}
                              side-name])
                           side-names))]
       [sf-burner-table (- width 40) (- height 150)
        row-count col-count
        value-fn on-change dual-nozzle?]
       [sf-burner-legend dual-nozzle?]])))

(defn sf-burner-entry [width height]
  (let [config @(rf/subscribe [::subs/config])
        ch-count (count (get-in config [:sf-config :chambers]))
        dual-nozzle? (get-in config [:sf-config :dual-nozzle?])
        render-fn (fn [indexes _]
                    (map (fn [ch-index]
                           (let [{:keys [burner-row-count burner-count-per-row]}
                                 (get-in config [:sf-config :chambers ch-index])]
                             [sf-chamber-burner-entry (- width 20) (- height 30)
                              (get-in config [:sf-config :chambers ch-index :name])
                              (get-in config [:sf-config :chambers ch-index :side-names])
                              ch-index dual-nozzle?
                              burner-row-count burner-count-per-row]))
                         indexes))]
    [lazy-cols {:width width, :height height
                :item-width width
                :item-count ch-count
                :items-render-fn render-fn}]))

(defn burner-entry [{{:keys [width height]} :view-size}]
  (if @(rf/subscribe [::subs/burner?])
    (if-let [firing @(rf/subscribe [::subs/firing])]
      (case firing
        "side" [sf-burner-entry width height]
        "top" [tf-burner-entry width height]))
    ;; burner entry not started yet
    [app-comp/button
     {:icon ic/plus
      :label (translate [:dataset :burner-entry :start] "Add burner data")
      :on-click #(rf/dispatch [::event/add-burners])}]))