(ns tta.component.dataset.twt-entry.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.input :refer [list-tube-both-sides
                                   list-wall-temps]]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.view :as app-view :refer [vertical-line]]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]))

(defn top-twt-entry-tube-row [_ config level row]
  (let [{:keys [name start-tube end-tube]}
        (get-in config [:tf-config :tube-rows row])
        ;;
        row-path [:top-fired :levels level :rows row]
        pref-fn (fn [index]
                  (get @(rf/subscribe [::subs/tube-prefs row]) index))
        field-fn (fn [index side]
                   @(rf/subscribe [::subs/field-temp
                                   [:top-fired :levels level :rows row :sides side
                                    :tubes index :raw-temp]]))
        on-change (fn [index side value]
                    (rf/dispatch [::event/set-temp
                                  (conj row-path :sides side :tubes index :raw-temp)
                                  value false]))
        on-clear #(rf/dispatch [::event/clear-raw-temps row-path])
        last? (= (inc row) (get-in config [:tf-config :tube-row-count]))]
    (fn [height _ _ _]
      [:div
       [list-tube-both-sides
        {:label name
         :height height
         :start-tube start-tube
         :end-tube end-tube
         :on-clear  (if @(rf/subscribe [::subs/has-raw-temp row-path])
                      on-clear)
         :field-fn field-fn
         :pref-fn pref-fn
         :on-change on-change}]
       (if-not last? [vertical-line {:height height}])])))

(defn top-twt-entry-wall [_ config wall-index]
  (let [wall-key (get [:north :east :south :west] wall-index)
        label (get-in config [:tf-config :wall-names wall-key])
        path [:top-fired :wall-temps wall-key :temps]
        field-fn #(deref (rf/subscribe [::subs/field-temp (conj path %)]))
        on-change (fn [index value]
                    (rf/dispatch [::event/set-temp (conj path index) value false]))
        on-clear #(rf/dispatch [::event/clear-wall-temps path])
        on-add #(rf/dispatch [::event/add-temp-field path])
        last? (= wall-key :west)]
    (fn [height _ _]
      [:div
       [list-wall-temps
        {:label label
         :height height
         :wall-count @(rf/subscribe [::subs/tf-wall-temps-count wall-key])
         :on-clear (if @(rf/subscribe [::subs/has-wall-temps path])
                     on-clear)
         :field-fn field-fn
         :on-add on-add
         :on-change on-change}]
       (if-not last? [vertical-line {:height height}])])))

(defn top-twt-entry-ceiling-floor [height config index]
  )

(defn top-twt-entry-full [{:keys [level], {:keys [width height]} :view-size}]
  (let [scope @(rf/subscribe [::subs/twt-entry-scope])
        config @(rf/subscribe [::subs/config])
        tr-count (get-in config [:tf-config :tube-row-count])
        item-count (case scope
                     :tube tr-count
                     :wall 4
                     :ceiling (inc tr-count)
                     :floor (inc tr-count))
        item-width (+ 20 (case scope
                           :tube 220
                           (:wall :ceiling :floor) 160))
        render-fn (let [height (- height 20)]
                    (case scope
                      :tube
                      (fn [indexes _]
                        (map (fn [row]
                               [top-twt-entry-tube-row height config level row])
                             indexes))
                      :wall
                      (fn [indexes _]
                        (map (fn [index]
                               [top-twt-entry-wall height config index])
                             indexes))
                      (:ceiling :floor)
                      (fn [indexes _]
                        (map (fn [index]
                               [top-twt-entry-ceiling-floor height config index])
                             indexes))))]
    [lazy-cols {:width width
                :height height
                :item-width item-width
                :item-count item-count
                :items-render-fn render-fn}]))

(defn top-twt-entry-partial [{:keys [level], {:keys [width height]} :view-size}]
  [:div {:style {:width width, :height height}} "top twt entry partial"])

(defn top-twt-entry [props]
  (let [mode @(rf/subscribe [::subs/twt-entry-mode])]
    (case mode
      :full [top-twt-entry-full props]
      ;; default
      :partial [top-twt-entry-partial props])))

(defn side-twt-entry [{:keys [view-size]}]
  [:div {:style view-size} "side"])

(defn twt-scope-selector []
  [:div "scope selector"])

(defn twt-entry [{:keys [level], {:keys [width height]} :view-size}]
  (let [firing @(rf/subscribe [::subs/firing])
        h2 48
        h1 (- height 48)
        props {:level level
               :view-size {:width width, :height h1}}]
    [:div
     [:div {:style {:height h1, :width width}}
      (case firing
        "top" [top-twt-entry props]
        "side" [side-twt-entry props])]
     [:div {:style {:height h2, :width width}}
      [twt-scope-selector]]]))
