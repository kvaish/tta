(ns tta.burner
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.style :refer [color color-hex color-rgba]]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :as scroll]))

(defonce my-state (r/atom {}))

(def tf-burner-style
  (let [circle {:width "32px", :height "32px"
                :display "inline-block"
                :border-radius "50%"
                :font-size "10px"
                :line-height "32px"
                :text-align "center"
                :vertical-align "top"
                :margin-top "8px"
                :color (color-hex :white)}
        palette-unit {}]
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
  "r 255 => 0, g 0 => 255 => 0, b 0 => 255 "
  (if (and opening (not= opening ""))
    (some (fn [i]
            (if (<= opening i)
              (get opening-color-map i)))
          (keys opening-color-map))
    #_(let [distribution [15 30 45 60 75 90]]
      (some (fn [i]
              (if (<= opening i)
                (let [r (js/Math.floor (- 255 (* i 2.84)))
                      g (if (<= i 45)
                          (js/Math.floor (* i 5.67))
                          (js/Math.floor (- 255 (* (- i 45) 5.67))))
                      b (js/Math.floor (* 2.84 i))]
                  (str "rgb(" r "," g "," b ")"))
                nil)) distribution))
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

(defn tf-burner [{:keys [value-fn label-fn on-change row col] :as props}]
  (let [state (r/atom props)
        style tf-burner-style
        on-change #(on-change row col %)]
    (fn [{:keys [value-fn label-fn]}]
      (let [value (value-fn row col)
            label (label-fn row col)]
        [:div (stylefy/use-style style)
         [:div (merge (stylefy/use-sub-style style :circle)
                      {:style {:background (opening->color value)}})
          label]
         [app-comp/text-input
          {:width     48
           :value     value
           :on-change on-change}]]))))

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
(defn sf-burner [{:keys [value on-change] :as props}]
  (let [style sf-burner-style
        state (r/atom props)
        mouse-enter #(swap! state assoc :hover? true)
        mouse-leave #(swap! state assoc :hover? false)]
    (fn [{:keys [value]}]
      (swap! state assoc :value value)
      [:div (merge (stylefy/use-style style)
                   {:on-mouse-enter mouse-enter
                    :on-mouse-leave mouse-leave})
       [:div (stylefy/use-sub-style style (keyword (or value "off")))]
       [sf-burner-popup {:state state}]])))

(defn burner-table-tf
  [width height row-count col-count value-fn label-fn on-change ]
  [:div {:style {:height 300 :padding "10px"}}
   [:div {:style {:font-weight "600"}} "Burner Rows"] [:br]
   [:div {:style {:width "80%" :float "left"}}
    [scroll/table-grid
     {:height              height :width width
      :row-header-width    0 :col-header-height 30
      :row-count           row-count :col-count col-count
      :row-height          48 :col-width 120
      :table-count         [1 1]
      :padding             [3 3 15 15]
      :row-header-renderer (fn [row [t-row t-col]]
                             nil)
      :col-header-renderer (fn [col [t-row t-col]]
                             [:div {:style {:text-align "center"}}
                              (inc col)])
      :cell-renderer       (fn [row col [t-row t-col]]
                             [tf-burner {:value-fn  value-fn
                                         :label-fn  label-fn
                                         :on-change on-change}])}]]
   [:div {:style {:width "15%" :float "left" :margin-left "20px"}}
    [color-palette]]])

(defn burner-tf-comp []
  [burner-table-tf
   500 300 10 7
   #(js/console.log %1 %2)
   #(js/console.log %1 %2)
   #(js/console.log %1 %2 %3) 400])

(defn burner-table-sf
  [width height row-count col-count value-fn on-change ]
  [:div {:style {:height 300 :padding "10px"}}
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
                                           :height "inherit"}}
                             (inc row)])
     :col-header-renderer (fn [col [t-row t-col]]
                            [:div {:style {:text-align "center"
                                           :border-bottom "1px solid"
                                           :height "inherit"}}
                             (inc col)])
     :cell-renderer       (fn [row col [t-row t-col]]
                             [sf-burner {:value-fn     value-fn
                                         :on-change    on-change
                                         :dual-nozzle? true}])}]])

(defn burner-sf-comp []
  [burner-table-sf
   700 300 10 15
   #(js/console.log %1 %2 %3)
   #(js/console.log %1 %2 %3 %4) 400])

(defn burner []
  #_[:div {:style {:height 300 :padding "50px"}}
   [color-palette]
   [:div
    {:style {:float "left"}}
    [sf-burner {:value     (get-in @my-state [:sf :burner])
                :on-change #(swap! my-state assoc-in [:sf :burner] %)}]

    [sf-burner {:value        (get-in @my-state [:sf :burner])
                :on-change    #(swap! my-state assoc-in [:sf :burner] %)
                :dual-nozzle? true}] [:br]

    [tf-burner {:value     (get-in @my-state [:tf :burner])
                :on-change #(swap! my-state assoc-in [:tf :burner] %)}]]]
  #_[burner-tf-comp]

  [burner-sf-comp])
