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

(defn toggle [{:keys [disabled? path style class-name]}]
  (let [on? (:value @(rf/subscribe [::value path]))
        t-style (if disabled?
                  (if on? app-style/toggle-on-d app-style/toggle-off-d)
                  (if on? app-style/toggle-on app-style/toggle-off))]
    [:span {:style (merge {:display "inline-block"
                           :padding "13px"
                           :vertical-align "top"} style)
            :class-name class-name
            :on-click (if-not disabled?
                        #(rf/dispatch [::set-value path
                                       {:value (not on?)
                                        :valid? true}]))}
     [:div (use-style t-style)
      [:span (use-sub-style t-style :label)
       (if on?
         (translate [:ht-comp :toggle :on] "on")
         (translate [:ht-comp :toggle :off] "off"))]
      [:div (use-sub-style t-style :circle)]]]))

(defn icon-button [{:keys [disabled? icon style on-click]}]
  (let [t-style (if disabled? app-style/icon-button-disabled
                    app-style/icon-button)]
    [ui/icon-button (merge (use-style t-style)
                           {:style style
                            :disabled disabled?
                            :on-click on-click})
     [icon (use-sub-style t-style :icon
                          {::stylefy/with-classes ["ht-ic-icon"]})]]))

(defn icon-button-2 [{:keys [disabled? icon style on-click]}]
  (let [t-style (if disabled? app-style/icon-button-2-disabled
                    app-style/icon-button-2)]
    [ui/icon-button (merge (use-style t-style)
                           {:style style
                            :disabled disabled?
                            :on-click on-click})
     [icon (use-sub-style t-style :icon
                          {::stylefy/with-classes ["ht-ic-icon"]})]]))

(defn selector [{:keys [disabled? path style class-name options item-width]}]
  (let [{:keys [index] :or {value (first options) index 0}}
        @(rf/subscribe [::value path])
        t-style (app-style/selector (not disabled?))]
    [:span {:style (merge {:display "inline-block"
                           :padding "10px"
                           :vertical-align "top"} style)
            :class-name class-name}
     (into
      [:div (update (use-style t-style) :style assoc
                    :width (+ 8 (* item-width (count options))))
       [:div (update (use-sub-style t-style :marker) :style assoc
                     :width item-width
                     :left (+ 4 (* index item-width)))]]
      (map (fn [o i]
             [:span (-> (use-sub-style t-style
                                       (if (= i index) :active-label
                                           :label))
                        (update :style assoc
                                :left (+ 4 (* i item-width))
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
