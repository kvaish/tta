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
            [tta.app.d3 :refer [d3-svg get-value]]
            [tta.app.charts :refer [d3-scale]]
            [tta.app.comp :as app-comp]
            [tta.app.input :refer [list-tf-burners]]
            [tta.app.scroll :as scroll]
            [tta.app.view :as app-view]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]
            [tta.component.dataset.style :as style]))

;; TOP FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def tf-burner-color-scale
  [[90 0 (color-hex :red)]
   [75]
   [60]
   [45 50 (color-hex :yellow)]
   [30]
   [15]
   [0 100 (color-hex :sky-blue)]])

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

(def tf-burners-dwg-node
  {:tag :g, :class :root
   :nodes [{:tag :rect, :class :box
            :data :box
            :attr {:x :x, :y :y, :rx 8, :ry 8, :width :w, :height :h
                   :stroke (color-hex :bitumen-grey)
                   :stroke-width 3, :fill "none"}}
           {:tag :line, :class :tube-row-lines
            :data :tube-row-lines, :multi? true
            :attr {:x1 :x, :x2 :x, :y1 :y1, :y2 :y2
                   :stroke (color-hex :slate-grey)
                   :stroke-width 8, :stroke-linecap "round"}}
           {:tag :text, :class :tube-row-names
            :data :tube-row-names, :multi? true
            :attr (assoc (label-attr :x :y true "middle")
                         :style "font-size:14px;fill:currentColor")
            :text :text}
           {:tag :text, :class :wall-names
            :data :wall-names, :multi? true
            :attr (assoc (label-attr :x :y :v? "middle")
                         :style "font-size:14px;font-weight:700;fill:currentColor")
            :text :text}]})

(defn tf-burners-dwg [width height
                      burner-first? burner-row-count tube-rows wall-names]
  (let [op 20, ip 30 ;; outer inner padding
        cx op, cy op, cw (- width op op), ch (- height op op)
        xm (+ cx (* cw 0.5)), ym (+ cy (* ch 0.5))
        bn burner-row-count, tn (count tube-rows)
        bw 80, tw 30
        ty1 (+ cy 20), ty2 (+ cy ch -20)
        txe0 (+ cx ip tw (if burner-first? bw 0))
        txe (map #(+ txe0 (* % (+ bw tw))) (range tn))
        data {:box {:x cx, :y cy, :w cw, :h ch}
              :tube-row-lines (map (fn [xe] {:x (- xe 6), :y1 ty1, :y2 ty2}) txe)
              :tube-row-names (map (fn [xe tr]
                                     {:x (- xe 16), :y ym, :text (:name tr)})
                                   txe tube-rows)
              :wall-names
              (let [{:keys [north south east west]} wall-names]
                [{:text north, :x xm, :y (- cy 6), :v? false}
                 {:text south, :x xm, :y (+ cy ch 16), :v? false}
                 {:text west, :x (- cx 6), :y ym, :v? true}
                 {:text east, :x (+ cx cw 16), :y ym, :v? true}])}]
    [:div {:style {:width width, :height height
                   :position "absolute", :top 0, :left 0}}
     [d3-svg {:view-box (str "0 0 " width" " height)
              :style    {:color (color-hex :bitumen-grey)
                         :user-select    "none"}
              :width width, :height height
              :node tf-burners-dwg-node
              :data data}]]))

(defn tf-burners-table [width height mode
                        burner-first? burner-count-per-row burner-row-count]
  (let [yp 50, xp 30, h (- height yp yp)
        [ds _ cs] (->> tf-burner-color-scale
                     (filter second)
                     (apply map list))
        color-scale (d3-scale ds cs)]
    [:div {:style {:width width, :height height
                   :position "absolute", :top 0, :left 0
                   :padding (str yp "px " xp "px")}}
     [list-tf-burners
      {:height h
       :item-count burner-count-per-row
       :row-count burner-row-count
       :burner-first? burner-first?
       :color-fn color-scale
       :field-fn (fn [bi bri] @(rf/subscribe [::subs/tf-burner bri bi]))
       :on-change (fn [bi bri value]
                    (rf/dispatch [::event/set-tf-burner bri bi value]))
       :read-only? (= mode :read)}]]))

(defn tf-burners [style mode]
  (let [{:keys [w1 h2]} (:data (meta style))
        {:keys [burner-first? burner-row-count burner-rows
                tube-row-count tube-rows
                wall-names]} (:tf-config @(rf/subscribe [::subs/config]))
        burner-count-per-row (get-in burner-rows [0 :burner-count])
        bw 80, tw 30, p 50 ;; 20+30
        w (+ (* burner-row-count bw) (* tube-row-count tw) p p)
        h (- h2 20)]
    [:div (use-sub-style style :burners)
     [scroll/scroll-box {:style {:width (min w w1), :height h2
                                 :margin "auto"}}
      [:div {:style {:width w, :height h
                     :position "relative"
                     :overflow "hidden"}}
       [tf-burners-dwg w h
        burner-first? burner-row-count tube-rows wall-names]
       [tf-burners-table w h mode
        burner-first? burner-count-per-row burner-row-count]]]]))

(defn tf-burner-legend [style]
  (let [{:keys [h2]} (:data (meta style))
        h (min h2 600), dh (/ h 8)
        p 20, w1 20, w2 20, w3 30
        w (+ p w1 w2 w3 p)
        x1 (+ p w1), x2 (+ x1 w2), x3 (+ x2 10)]
    [:div (use-sub-style style :legend)
     [:svg {:view-box (str "0 0 " w " " h)
            :width w, :height h
            :style {:font-size "12px"}}
      [:defs
       (->> tf-burner-color-scale
            (filter second)
            (map (fn [[_ pct color]]
                   [:stop {:offset (str pct "%")
                           :stop-color color}]))
            (into [:linearGradient {:id "tf-burner-color-scale"
                                    :x1 "0%", :x2 "0%", :y1 "0%", :y2 "100%"}]))]
      (->> tf-burner-color-scale
           (mapcat (fn [i [deg]]
                     (let [y (* dh (inc i))]
                       [[:line {:x1 x1, :x2 (+ x2 6), :y1 y, :y2 y
                                :stroke (color-hex :bitumen-grey)}]
                        [:text {:x x3, :y (+ y 5)
                                :font-size "12px"
                                :fill (color-hex :bitumen-grey)}
                         (str deg  "°")]]))
                   (range))
           (into [:g
                  (let [x (- x1 10), y (/ h 2)]
                    [:text {:x x, :y y, :fill (color-hex :sky-blue)
                            :text-anchor "middle"
                            :transform (str "rotate (270," x "," y ")")}
                     (translate [:dataset :burner :valve-opening]
                                "Burner valve opening")])
                  [:rect {:x x1, :y dh, :width w2, :height (* 6 dh)
                          :fill "url(#tf-burner-color-scale)"}]]))]]))

(defn tf-burner-header [style]
  ;; value is ensured to be valid always
  (let [value @(rf/subscribe [::subs/tf-burners-fill-all])]
    [:div (use-sub-style style :header)
     [:div (use-sub-style style :fill-all)
      [app-comp/text-input
       {:on-change #(rf/dispatch [::event/set-tf-burners-fill-all %])
        :width 56, :value value}]
      [app-comp/button
       {:disabled? (not value)
        :icon ic/dataset
        :label (translate [:action :fill-all :label] "Fill all")
        :on-click #(rf/dispatch [::event/fill-all-tf-burners value])}]]]))

(defn tf-burner-entry [width height]
  (let [style (style/tf-burners-layout width height :edit)]
    [:div (use-style style)
     [tf-burner-header style]
     [tf-burners style :edit]
     [tf-burner-legend style]]))


;; SIDE FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sf-burner-popup [{:keys [state]}]
  (let [style style/sf-burner-style
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
  (let [style style/sf-burner-style
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
  "TODO: doc the fn signatures
  **value-fn**
  **on-change**"
  [width height row-count col-count value-fn on-change dual-nozzle? burner-no]
  [scroll/table-grid
   {:height height, :width width
    :row-header-width 64, :col-header-height 30
    :row-count (inc row-count,) :col-count (inc col-count)
    :row-height 64, :col-width 64
    :labels ["Row" "Burner"]
    :gutter [20 20]
    :table-count [1 2]
    :padding [3 3 15 15]
    :row-header-renderer (fn [row [t-row t-col]]
                           [:div (use-sub-style style/sf-burner-table :row-head)
                            (if (< row row-count) (inc row))])
    :col-header-renderer (fn [col [t-row t-col]]
                           [:div (use-sub-style style/sf-burner-table :col-head)
                            (if (< col col-count) (burner-no col))])
    :cell-renderer (fn [row col [t-row t-col]]
                     (if (and (< row row-count)
                              (< col col-count))
                       [sf-burner {:value-fn value-fn
                                   :on-change on-change
                                   :dual-nozzle? dual-nozzle?
                                   :row row
                                   :col col
                                   :side t-col}]))}])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn sf-burner-legend [dual-nozzle?]
  (let [style style/sf-burner-style]
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
                               row-count col-count burner-no]
  (let [value-fn (fn [side row col]
                   @(rf/subscribe [::subs/sf-burner [ch-index side row col]]))
        on-change (fn [side row col value]
                    (rf/dispatch [::event/set-sf-burner
                                  [ch-index side row col] value]))]
    (fn [width height _ _]
      (let [w (- width 40)
            w2 (/ w 2)
            h1 60
            h2 (- height h1 48 40)]
        [:div (update (use-sub-style style/sf-burner-table :body) :style
                      assoc :width width, :height height)
         [:div (update (use-sub-style style/sf-burner-table :title) :style
                       assoc :width w, :height h1)
          [:div {:style {:width w}} ch-name]
          (doall
           (map-indexed (fn [i side-name]
                          [:div {:key i
                                 :style {:width w2, :display "inline-block"}}
                           side-name])
                        side-names))]
         [sf-burner-table w h2
          row-count col-count
          value-fn on-change dual-nozzle? burner-no]
         [sf-burner-legend dual-nozzle?]]))))

(defn sf-burner-entry [width height]
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
                             [sf-chamber-burner-entry (- width 20) (- height 30)
                              name side-names
                              ch-index dual-nozzle?
                              burner-row-count burner-count-per-row
                              burner-no]))
                         indexes))]
    [scroll/lazy-cols {:width width, :height height
                       :item-width width
                       :item-count ch-count
                       :items-render-fn render-fn}]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn burner-entry [{{:keys [width height]} :view-size}]
  (if @(rf/subscribe [::subs/burner?])
    (let [firing @(rf/subscribe [::subs/firing])]
      (case firing
        "side" [sf-burner-entry width height]
        "top" [tf-burner-entry width height]))
    ;; burner entry not started yet
    [app-comp/button
     {:icon ic/plus
      :label (translate [:dataset :burner-entry :start] "Add burner data")
      :on-click #(rf/dispatch [::event/add-burners])}]))
