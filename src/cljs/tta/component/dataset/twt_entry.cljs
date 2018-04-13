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
            [tta.app.comp :as app-comp]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.view :as app-view :refer [vertical-line]]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]
            [tta.app.icon :as ic]))

(defn tf-twt-entry-tube-row [_ info level-key row last?]
  (let [{:keys [name start-tube end-tube]} info
        row-path [:top-fired :levels level-key :rows row]
        pref-fn (fn [index]
                  (get @(rf/subscribe [::subs/tube-prefs row]) index))
        field-fn (fn [index side]
                   @(rf/subscribe [::subs/field-temp
                                   (conj row-path :sides side :tubes index :raw-temp)]))
        on-change (fn [index side value]
                    (rf/dispatch [::event/set-temp
                                  (conj row-path :sides side :tubes index :raw-temp)
                                  value false]))
        on-clear #(rf/dispatch [::event/clear-raw-temps row-path])]
    (fn [height _ _ _ _]
      [:span
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

(defn tf-twt-entry-ceiling-floor [_ scope row last?]
  (let [path [:top-fired (case scope
                           :ceiling :ceiling-temps
                           :floor :floor-temps
                           nil)
              row :temps]
        label (str
               (case scope
                 :ceiling (translate [:twt-entry :twt-scope :ceiling] "Ceiling")
                 :floor (translate [:twt-entry :twt-scope :floor] "Floor")
                 nil)
               " " (inc row))
        field-fn #(deref (rf/subscribe [::subs/field-temp (conj path %)]))
        on-change (fn [index value]
                    (rf/dispatch [::event/set-temp (conj path index) value false]))
        on-clear #(rf/dispatch [::event/clear-wall-temps path])
        on-add #(rf/dispatch [::event/add-temp-field path])]
    (fn [height _ _ _]
      [:span
       [list-wall-temps
        {:label label
         :height height
         :wall-count @(rf/subscribe [::subs/wall-temps-count path])
         :on-clear (if @(rf/subscribe [::subs/has-wall-temps path])
                     on-clear)
         :field-fn field-fn
         :on-add on-add
         :on-change on-change}]
       (if-not last? [vertical-line {:height height}])])))

(defn tf-twt-entry-wall [_ label wall-key last?]
  (let [path [:top-fired :wall-temps wall-key :temps]
        field-fn #(deref (rf/subscribe [::subs/field-temp (conj path %)]))
        on-change (fn [index value]
                    (rf/dispatch [::event/set-temp (conj path index) value false]))
        on-clear #(rf/dispatch [::event/clear-wall-temps path])
        on-add #(rf/dispatch [::event/add-temp-field path])]
    (fn [height _ _ _]
      [:span
       [list-wall-temps
        {:label label
         :height height
         :wall-count @(rf/subscribe [::subs/wall-temps-count path])
         :on-clear (if @(rf/subscribe [::subs/has-wall-temps path])
                     on-clear)
         :field-fn field-fn
         :on-add on-add
         :on-change on-change}]
       (if-not last? [vertical-line {:height height}])])))

(defn tf-twt-entry-full [{:keys [level-key]
                          {:keys [width height]} :view-size}]
  (let [scope @(rf/subscribe [::subs/twt-entry-scope level-key])
        config @(rf/subscribe [::subs/config])
        tr-count (get-in config [:tf-config :tube-row-count])
        item-count (case scope
                     :tube tr-count
                     :wall 4
                     (:ceiling :floor) (inc tr-count))
        last? #(= (inc %) item-count)
        item-width (+ 20 (case scope
                           :tube 220
                           (:wall :ceiling :floor) 160))
        render-fn (let [height (- height 20)]
                    (case scope
                      :tube
                      (fn [indexes _]
                        (map (fn [row]
                               [tf-twt-entry-tube-row
                                height
                                (get-in config [:tf-config :tube-rows row])
                                level-key row (last? row)])
                             indexes))
                      :wall
                      (fn [indexes _]
                        (map (fn [index]
                               (let [wall-key (get [:north :east :south :west] index)]
                                 [tf-twt-entry-wall
                                  height
                                  (get-in config [:tf-config :wall-names wall-key])
                                  wall-key (last? index)]))
                             indexes))
                      (:ceiling :floor)
                      (fn [indexes _]
                        (map (fn [row]
                               [tf-twt-entry-ceiling-floor
                                height scope row (last? row)])
                             indexes))
                      nil))]
    [lazy-cols {:width width
                :height height
                :item-width item-width
                :item-count item-count
                :items-render-fn render-fn}]))

(defn tf-twt-entry-partial [{:keys [level-key]
                             {:keys [width height]} :view-size}]
  (let [width (/ width 2)
        scope @(rf/subscribe [::subs/twt-entry-scope level-key])
        config @(rf/subscribe [::subs/config])
        index @(rf/subscribe [::subs/twt-entry-index scope])]
    [:span
     [:div {:style {:width width, :height height
                    :display "inline-block"}}
      "interactive"]
     [:div {:style {:width width, :height height
                    :display "inline-block"}}
      [app-comp/icon-button-l
       {:icon ic/nav-left
        :disabled? @(rf/subscribe [::subs/twt-entry-nav-disabled? scope :prev])
        :on-click #(rf/dispatch [::event/move-twt-entry-index scope :prev])}]
      [vertical-line {:height height}]
      (case scope
        :tube
        ^{:key index} [tf-twt-entry-tube-row height
                       (get-in config [:tf-config :tube-rows index])
                       level-key index false]
        :wall
        ^{:key index} [tf-twt-entry-wall
                       height
                       (get-in config [:tf-config :wall-names index])
                       index false]
        (:ceiling :floor)
        ^{:key index} [tf-twt-entry-ceiling-floor height scope index false]
        nil)
      [app-comp/icon-button-l
       {:icon ic/nav-right
        :disabled? @(rf/subscribe [::subs/twt-entry-nav-disabled? scope :next])
        :on-click #(rf/dispatch [::event/move-twt-entry-index scope :next])}]]]))

(defn tf-twt-entry [props]
  (let [mode @(rf/subscribe [::subs/twt-entry-mode])]
    (case mode
      :full [tf-twt-entry-full props]
      ;; default
      :partial [tf-twt-entry-partial props])))

(defn sf-twt-entry [{:keys [view-size]}]
  [:div {:style view-size} "side"])

(defn twt-scope-selector [{:keys [level-key]}]
  (let [opts @(rf/subscribe [::subs/twt-entry-scope-opts level-key])
        sel @(rf/subscribe [::subs/twt-entry-scope level-key])
        sel (some #(if (= sel (:id %)) %) opts)
        firing @(rf/subscribe [::subs/firing])
        mode @(rf/subscribe [::subs/twt-entry-mode])]
    [:div {:style {:position "absolute"
                   :right 0, :top 0}}
     [app-comp/selector
      {:item-width 60
       :selected sel
       :options opts
       :on-select #(rf/dispatch [::event/set-twt-entry-scope level-key (:id %)])
       :label-fn :label}]
     (if (= "top" firing)
       [app-comp/icon-button-l
        {:icon ic/dataset
         :tooltip (case mode
                    :partial (translate [:twt-entry :switch-to-full :tooltip]
                                        "switch to full view")
                    (translate [:twt-entry :switch-to-partial :tooltip]
                               "switch to partial view"))
         :on-click #(rf/dispatch [::event/set-twt-entry-mode
                                  (case mode
                                    :partial :full
                                    :partial)])}])]))

(defn twt-entry [{:keys [level], {:keys [width height]} :view-size}]
  (let [firing @(rf/subscribe [::subs/firing])
        h2 48
        h1 (- height 48)
        level-key @(rf/subscribe [::subs/level-key level])]
    [:div
     [:div {:style {:height h1, :width width}}
      (let [props {:level-key level-key
                   :view-size {:width width, :height h1}}]
        (case firing
          "top" [tf-twt-entry props]
          "side" [sf-twt-entry props]))]
     [:div {:style {:height h2, :width width
                    :position "relative"}}
      [twt-scope-selector {:level-key level-key}]]]))
