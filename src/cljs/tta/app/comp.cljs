(ns tta.app.comp
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.app.subs :refer [translate]]
            [ht.app.style :as ht-style]
            [ht.app.icon :as ht-ic]
            [tta.app.style :as app-style]
            [tta.app.icon :as ic]
            [ht.util.interop :as i]
            [reagent.dom :as dom]))

(defn popover [props & children]
  (into [ui/popover (merge props (use-style app-style/popover))]
        children))

;; 72x48
(defn toggle [{:keys [disabled? value on-toggle]}]
  (let [on? value
        style (app-style/toggle on? disabled?)]
    [:span (-> (use-style style)
               (assoc :on-click
                      (if-not disabled?
                        #(on-toggle (not on?)))))
     [:div (use-sub-style style :main)
      [:span (use-sub-style style :label)
       (if on?
         (translate [:ht-comp :toggle :on] "on")
         (translate [:ht-comp :toggle :off] "off"))]
      [:div (use-sub-style style :circle)]]]))

;; 48x48, icon: 24x24, back: 32x32
(defn icon-button-l [{:keys [disabled? icon on-click]}]
  [ui/icon-button {:disabled disabled?
                   :on-click on-click
                   :style {:vertical-align "top"}}
   [icon (-> (use-style (app-style/icon-button disabled?))
             (update :style assoc :padding "4px"
                     :width "32px", :height "32px"
                     :margin "-4px"))]])

;; 48x48, icon: 24x24
(defn icon-button [{:keys [disabled? icon on-click]}]
  [ui/icon-button {:disabled disabled?
                   :on-click on-click}
   [icon (use-style (app-style/icon-button disabled?))]])

;; 48x48, icon: 22x22
(defn icon-button-s [{:keys [disabled? icon on-click]}]
  [ui/icon-button {:disabled disabled?
                   :on-click on-click}
   [icon (-> (use-style (assoc (app-style/icon-button disabled?)
                               :width "22px" :height "22px"
                               :margin "1px"))
             (assoc :view-box "1 1 22 22"))]])

;; *x48
(defn button [{:keys [disabled? label icon on-click]}]
  (let [style (app-style/button disabled?)]
    [:span (use-style (:container style))
     [ui/flat-button {:style (:btn style)
                      :disabled disabled?
                      :on-click on-click
                      :hover-color (:hc style)
                      :background-color (:bg style)}
      [:div (use-style (:div style))
       [icon (use-style (:icon style))]
       [:span (use-style (:span style)) label]]]]))

;; *x48
(defn selector [{:keys [disabled? valid? item-width selected options on-select]
                 :or [valid? true]}]
  (let [index (some #(if (= selected (first %)) (second %))
                    (map list options (range)))
        style (app-style/selector disabled? valid?)]
    [:span (use-style style)
     (into
      [:div (update (use-sub-style style :main) :style assoc
                    :width (+ 10 (* item-width (count options))))
       (if index
         [:div (update (use-sub-style style :marker) :style assoc
                       :width item-width
                       :left (+ 4 (* index item-width)))])]
      (map (fn [o i]
             [:span (-> (use-sub-style style
                                       (if (= i index) :active-label
                                           :label))
                        (update :style assoc
                                :left (+ 4 (* i item-width))
                                :width item-width)
                        (assoc :on-click (if-not disabled? #(on-select o i))))
              o])
           options (range)))]))

(defn text-input [{:keys [read-only? valid? align width value on-change]
                   :or {valid? true, width 96, align "left"}}]
  (let [style (app-style/text-input read-only? valid?)]
    [:span (use-style style)
     [:input (-> (use-sub-style style :main)
                 (update :style assoc
                         :width width
                         :text-align align)
                 (merge {:type "text"
                         :value value
                         :on-change #(on-change (i/oget-in % [:target :value]))
                         :read-only read-only?}))]]))

(defn action-input-box [{:keys [disabled? valid? width label action
                                left-icon left-action left-disabled?
                                right-icon right-action right-disabled?]
                         :or {valid? true}}]
  (let [style (app-style/action-input-box disabled? valid? (some? action)
                                          left-disabled? right-disabled?)
        align (if (and left-icon right-icon) "center" "left")]
    [:span (use-style style)
     [:div (use-sub-style style :main)
      (if left-icon
        [left-icon (merge (use-sub-style style :left)
                          {:on-click left-action})])
      [:span (-> (use-sub-style style :span)
                 (update :style assoc
                         :width width
                         :text-align align)
                 (merge {:on-click action}))
       label]
      (if right-icon
        [right-icon (merge (use-sub-style style :right)
                           {:on-click right-action})])]]))

;; *x48
(defn dropdown-selector [props]
  (let [state (r/atom {})]
    (r/create-class
     {:component-did-mount (fn [this]
                             (swap! state assoc :anchor (dom/dom-node this)))
      :reagent-render
      (fn [{:keys [disabled? valid? width on-select
                  left-icon left-action left-disabled?
                  selected items value-fn label-fn disabled?-fn]
           :or {valid? true, disabled?-fn (constantly false)
                value-fn identity, label-fn identity}}]
        (let [{:keys [open? anchor]} @state
              action #(do
                        (i/ocall % :preventDefault)
                        (swap! state assoc :open? true))]
          [:span {:style {:display "inline-block"
                          :padding "0"
                          :vertical-align "top"}}
           [action-input-box
            (cond-> {:disabled? disabled?
                     :valid? valid?
                     :label (label-fn selected)
                     :width width
                     :action action
                     :right-icon ic/dropdown
                     :right-action action}
              left-icon (assoc :left-icon left-icon
                               :left-action left-action
                               :left-disabled? left-disabled?))]
           (if open?
             [popover {:open true
                       :on-request-close #(swap! state assoc :open? false)
                       :anchor-el anchor
                       :anchor-origin {:horizontal "right", :vertical "bottom"}
                       :target-origin {:horizontal "right", :vertical "top"}}
              (into [ui/menu {:value (value-fn selected)
                              :menu-item-style {:font-size "12px"
                                                :line-height "24px"
                                                :min-height "24px"}}]
                    (map (fn [item i]
                           [ui/menu-item
                            {:key i
                             :primary-text (label-fn item)
                             :on-click #(do
                                          (swap! state assoc :open? false)
                                          (on-select item i))
                             :disabled (disabled?-fn item)
                             :value (value-fn item)}])
                         items (range)))])]))})))
