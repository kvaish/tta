(ns tta.burner
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.style :refer [color color-hex color-rgba]]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]))

(defonce my-state (r/atom {}))

;; 48x48
(def sf-burner-style
  (let [col-on (color-hex :green)
        col-off (color-hex :red)
        col-off-aux (color-hex :orange)
        col-off-fix (color-hex :brown)
        circle {:width "24px", :height "24px"
                :position "absolute"
                :top "12px", :left "12px"
                :border-radius "50%"
                :background (color-hex :white)
                :box-shadow "inset -6px -6px 10px rgba(0,0,0,0.3)"}]
    {:height "48px", :width "48px"
     :display "inline-block"
     :background "#eee"
     :position "relative"
     ::stylefy/sub-styles
     {:on (assoc circle :background col-on)
      :off (assoc circle :background col-off)
      :off-aux (assoc circle :background col-off-aux)
      :off-fix (assoc circle :background col-off-fix)
      :popup {:position "absolute"
              :height "96px", :width "96px"
              :top "-24px", :left "-24px"
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

(defn burner []
  [:div {:style {:height 300
                 :padding "50px"}}
   [sf-burner {:value (get-in @my-state [:sf :burner])
               :on-change #(swap! my-state assoc-in [:sf :burner] %)}]
   [sf-burner {:value (get-in @my-state [:sf :burner])
               :on-change #(swap! my-state assoc-in [:sf :burner] %)
               :dual-nozzle? true}]])
