(ns tta.app.comp
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.app.subs :refer [translate]]
            [ht.app.style :as ht-style]
            [ht.app.icon :as ht-ic]
            [tta.app.style :as app-style]
            [tta.app.icon :as ic]))

(rf/reg-sub
 ::value
 (fn [db [_ path]]
   (get-in db path)))

(rf/reg-event-db
 ::set-value
 (fn [db [_ path value]]
   (assoc-in db path value)))

;; 72x48
(defn toggle [{:keys [disabled? path]}]
  (let [on? (:value @(rf/subscribe [::value path]))
        style (app-style/toggle on? disabled?)]
    [:span (-> (use-sub-style style :container)
               (assoc :on-click
                      (if-not disabled?
                        #(rf/dispatch [::set-value path {:value (not on?)
                                                         :valid? true}]))))
     [:div (use-style style)
      [:span (use-sub-style style :label)
       (if on?
         (translate [:ht-comp :toggle :on] "on")
         (translate [:ht-comp :toggle :off] "off"))]
      [:div (use-sub-style style :circle)]]]))

;; 48x48, icon: 24x24
(defn icon-button [{:keys [disabled? icon on-click]}]
  [ui/icon-button {:disabled disabled?
                   :on-click (or on-click (fn []))}
   [icon (use-style (app-style/icon-button disabled?))]])

;; 48x48, icon: 22x22
(defn icon-button-s [{:keys [disabled? icon on-click]}]
  [ui/icon-button {:disabled disabled?
                   :on-click (or on-click (fn []))}
   [icon (-> (use-style (assoc (app-style/icon-button disabled?)
                               :width "22px" :height "22px"
                               :margin "1px"))
             (assoc :view-box "1 1 23 23"))]])

;; *x48
(defn button [{:keys [disabled? label icon on-click]}]
  (let [style (app-style/button disabled?)]
    [ui/flat-button {:style (:btn style)
                     :disabled disabled?
                     :on-click on-click
                     :hover-color (:hc style)
                     :background-color (:bg style)}
     [:div {:style (:div style)}
      [icon {:style (:icon style)}]
      [:span {:style (:span style)} label]]]))

;; *x48
(defn selector [{:keys [disabled? path options item-width]}]
  (let [{:keys [index] :or {value (first options) index 0}}
        @(rf/subscribe [::value path])
        style (app-style/selector disabled?)]
    [:span (use-sub-style style :container)
     (into
      [:div (update (use-style style) :style assoc
                    :width (+ 7 (* item-width (count options))))
       [:div (update (use-sub-style style :marker) :style assoc
                     :width item-width
                     :left (+ 3 (* index item-width)))]]
      (map (fn [o i]
             [:span (-> (use-sub-style style
                                       (if (= i index) :active-label
                                           :label))
                        (update :style assoc
                                :left (+ 3 (* i item-width))
                                :width item-width)
                        (assoc :on-click
                               (if-not disabled?
                                 #(rf/dispatch [::set-value path
                                                {:value o
                                                 :index i
                                                 :valid? true}]))))
              o])
           options (range)))]))

(defn popover [props & children]
  [ui/popover (assoc props :style {:background "none"
                                   :border "none"
                                   :box-shadow "none"})
   [:div {:style {:height 0, :width 0
                  :position "absolute"
                  :top -8, :right 25
                  :border "8px solid rgba(0,0,0,0)"
                  :border-bottom-color "white"}}]
   [:div {:style {:height 5, :width 0}}]
   (into [:div {:style {:background "white"
                        :box-shadow "rgba(0,0,0,0.12) 2px 2px 16px 2px,
rgba(0,0,0,0.12) 2px 2px 8px 2px"
                        :border-radius "5px"
                        :margin "3px 15px 15px 15px"}}]
         children)])
