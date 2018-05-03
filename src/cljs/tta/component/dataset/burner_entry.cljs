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
            [tta.app.input :as input]
            [tta.app.scroll :as scroll :refer [lazy-cols]]
            [tta.app.view :as app-view]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]
            [tta.component.dataset.style :as style]
            [tta.app.d3 :refer [d3-svg get-value]]))

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
                     :width "24px", :height "25px"}
      :temp-scale   {:vertical-align "top"
                     :margin-left "5px", :font-size "12px"}}}))

(def fill-all-field-style
  {:display "inline-block"
   :position "relative"
   :float "right"
   :padding "0 0 8px 12px"
   ::stylefy/sub-styles
   {:label {:font-size "12px"
            :font-weight 300
            :margin-top "14px"
            :color (color-hex :royal-blue)
            :vertical-align "top"
            :display "inline-block"}
    :error {:position "absolute"
            :color "red"
            :font-size "10px"
            :bottom 0, :left "12px"}}})

(def opening-color-map
  {5 "rgb(255, 0, 0)"
   10 "rgb(255, 0, 64)"
   15 "rgb(255, 0, 191)"
   20 "rgb(255, 0, 255)"
   25 "rgb(255, 128, 0)"
   30 "rgb(255, 191, 0)"
   35 "rgb(255, 255, 0)"
   40 "rgb(191, 255, 0)"
   45 "rgb(0, 255, 0)"
   50 "rgb(0, 255, 255)"
   55 "rgb(0, 191, 255)"
   60 "rgb(128, 128, 255)"
   65 "rgb(77, 77, 255)"
   70 "rgb(0, 64, 255)"
   75 "rgb(0, 0, 255)"
   80 "rgb(0, 0, 77)"
   85 "rgb(0, 0, 51)"
   90 "rgb(0, 0, 26)"})

(defn opening->color [opening]
  (if (and opening (not= opening ""))
    (some (fn [i]
            (if (<= opening i)
              (get opening-color-map i)))
          (sort (keys opening-color-map)))
    "#fff"))

(defn color-palette []
  (let [style tf-burner-style
        distribution (reverse (map #(* (inc %) 5) (range 18)))]
    [:div (stylefy/use-sub-style style :palette)
     (doall
      (map (fn [i] ^{:key i}
             [:div {:style {:height "20px"}}
              [:div (merge (stylefy/use-sub-style style :palette-unit)
                           {:style {:background (opening->color i)}})]
              [:span (stylefy/use-sub-style style :temp-scale) (str i "°")]])
           distribution))
     [:span (merge (stylefy/use-sub-style style :temp-scale)
                   {:style {:float "right"
                            :padding-right "6px"
                            :margin-top "-7px"}}) "0°"]]))

(defn label-attr
  ([x y] (label-attr x y false nil))
  ([x y v?] (label-attr x y v? nil))
  ([x y v? a]
   {:stroke "none", :fill "black"
    :x x, :y y
    :transform #(if (get-value v? %1 %2 %3)
                  (let [x (get-value x %1 %2 %3)
                        y (get-value y %1 %2 %3)]
                    (str "rotate(270," x "," y ")")))
    :text-anchor a}))

(defn tf-nodes [ch-width ch-height]
  (let [w ch-width, h ch-height, m 30, p 20
        x 80, y m, w (- w x x), h (- h m m)
        xe (+ x w), ye (+ y h)
        xm (+ x (/ w 2)), ym (+ y (/ h 2))
        y1 (+ y p), y2 (- ye p)]
    [{:tag   :g, :class :reformer
      :nodes [;; chamber box
              {:tag  :rect, :class :ch-box
               :attr {:x x, :y y, :width w, :height h
                      :stroke "black", :stroke-width "5px"}}
              ;; wall label
              {:tag    :text, :class :w-label
               :attr   (merge (label-attr :x :y :v? :a)
                              {:font-size "18px"})
               :text   :text
               :multi? true
               :data   (fn [c] (map (fn [[key value]]
                                      (let [[x y v?] (case key
                                                       :north [xm (- y 6) false]
                                                       :south [xm (+ ye 19) false]
                                                       :east [(+ xe 19) ym true]
                                                       :west [(- x 6) ym true]
                                                       nil)]
                                        {:text value
                                         :x    x, :y y, :v? v?, :a "middle"}))
                                    (get-in c [:tf-config :wall-names])))}
              ;; chamber group
              {:tag   :g, :class :chamber
               :data  (fn [c] (let [{tc :tube-row-count, bc :burner-row-count
                                     :keys [tube-rows burner-rows]} (:tf-config c)
                                    sc (+ tc 1)
                                    sp (/ w sc)
                                    xt (map #(+ x sp (* sp %)) (range tc))]
                                {:xt-pos  xt
                                 :tubes   tube-rows}))
               :nodes [;; row labels
                       {:tag    :text, :class :r-label
                        :attr   (merge
                                  (label-attr #(- (:x %) 6) y1 true "end")
                                  {:font-size "16px"
                                   :stroke    "black"})
                        :text   :text
                        :multi? true
                        :data   (fn [{:keys [xt-pos tubes]}]
                                  (map (fn [x {name :name}]
                                         {:x x :text name})
                                       xt-pos tubes))}
                       ;; tube row
                       {:tag    :line, :class :t-row
                        :attr   {:x1             identity, :y1 y1, :x2 identity, :y2 y2
                                 :stroke-width   "5px"
                                 :stroke-linecap "round"}
                        :multi? true
                        :data   :xt-pos}]}]}]))

(defn dwg [ch-width ch-height]
  {:view-box (str "0 0 " ch-width" " ch-height)
   :style    {:color          "grey"
              :user-select    "none"
              :fill           "none"
              :stroke         "grey"
              :stroke-width   "1px"
              :font-size      "14px"
              :font-family    "open_sans"
              :vertical-align "top"}
   :node     {:tag   :g, :class :reformer
              :nodes [{:tag  :rect, :class :back
                       :attr {:x     0, :y 0
                              :width ch-width, :height ch-height
                              :fill  "lightgrey"}}
                      {:tag   :g, :class :top-fired
                       :nodes (tf-nodes ch-width ch-height)}]}})

(defn tf-svg [{:keys [width height]}]
  [d3-svg (merge (dwg width height)
                 {:height height
                  :width  width
                  :data   @(rf/subscribe [::subs/config])})])

(defn fill-all [mode]
  (let [{:keys [value valid? error]} @(rf/subscribe [::subs/field [:top-fired :burners :fill-all]])
        error (if (fn? error) (error) error)
        style fill-all-field-style]
    [:div (use-style style)
     [app-comp/text-input
      {:on-change (fn [value]
                    (if (<= 0 (js/Number value) 90)
                      (rf/dispatch [::event/set-fill-all-field value])))
       :value value, :valid? valid?
       :read-only? (if-not (= :edit mode) true)}]
     [:span (use-sub-style style :error) error]
     [app-comp/button
      {:disabled? (or (not valid?)
                      (empty? value))
       :icon      ic/dataset
       :label     (translate [:action :fill-all :label] "Fill all")
       :on-click  #(rf/dispatch [::event/fill-all])}]]))

(defn tf-burner-table
  [width height
   burner-row-count burner-count-per-row
   wall-labels tube-row-count burner-first?]
  (let [w2 150
        w1 (- width w2)
        h (- height 48)
        mode @(rf/subscribe [::subs/mode])]
    [:div {:style {:height    height
                   :width     width
                   :font-size "12px"}}
     [:div {:style {:width w1, :height height
                    :display "inline-block"
                    :vertical-align "top"}}
      ;;fill all
      [fill-all mode]
      (let [x-offset 150
            ch-width (js/Math.ceil (* x-offset (inc tube-row-count)))
            w-new ch-width]
        [:div {:style {:width w-new, :margin "auto"}}
         ;; scrolling area
         [scroll/scroll-box {:style {:width w1, :height (- h 20)}}

          [:div {:style {:position "relative"
                         :padding  "5px"}}
           ;; tube rows svg
           [:div {:style {:position "absolute"}}
            [tf-svg {:width w-new, :height (- h 60)}]]

           ;; burner input
           [:div {:style {:margin-top "50px" :width w-new}}
            [input/list-burner-input
             {:height        (- h 120)
              :width         (+ w-new 50)
              :item-count    burner-count-per-row
              :row-count     burner-row-count
              :burner-first? burner-first?
              :field-fn      #(deref (rf/subscribe [::subs/tf-burner [%2 %1]]))
              :on-change     (fn [row index value]
                               (if (<= 0 (js/Number value) 90)
                                 (rf/dispatch [::event/set-tf-burner index row value])))
              :color-fn      opening->color
              :input-read-only? (if-not (= :edit mode) true)}]]]]])]
     [:div {:style {:width w2, :height height
                    :display "inline-block"
                    :vertical-align "top"
                    :padding "10px 0 0 60px"}}
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
  (let [{:keys [burner-row-count burner-rows
                wall-names tube-row-count burner-first?]}
        (:tf-config @(rf/subscribe [::subs/config]))
        burner-count (get-in burner-rows [0 :burner-count])]
    (fn [_ _]
      [tf-burner-table
       width height
       burner-row-count burner-count
       wall-names tube-row-count burner-first?])))

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
    (let [firing @(rf/subscribe [::subs/firing])
          mode @(rf/subscribe [::subs/mode])]
      (case firing
        "side" [sf-burner-entry width height]
        "top" [tf-burner-entry width height false]))
    ;; burner entry not started yet
    [app-comp/button
     {:icon ic/plus
      :label (translate [:dataset :burner-entry :start] "Add burner data")
      :on-click #(rf/dispatch [::event/add-burners])}]))
