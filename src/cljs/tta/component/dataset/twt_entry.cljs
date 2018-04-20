(ns tta.component.dataset.twt-entry
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-time.core :as t]
            [ht.style]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.input :refer [list-tube-both-sides
                                   list-wall-temps]]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :refer [lazy-cols]]
            [tta.app.view :as app-view :refer [vertical-line]]
            [tta.component.dataset.style :as style]
            [tta.component.dataset.subs :as subs]
            [tta.component.dataset.event :as event]
            [tta.component.dataset.twt-selector :refer [twt-selector]]))

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

(defn ceiling-floor-label [scope]
  (case scope
    :ceiling (translate [:twt-entry :twt-scope :ceiling] "Ceiling")
    :floor (translate [:twt-entry :twt-scope :floor] "Floor")
    nil))

(defn tf-twt-entry-ceiling-floor [_ scope row last?]
  (let [path [:top-fired (case scope
                           :ceiling :ceiling-temps
                           :floor :floor-temps
                           nil)
              row :temps]
        label (str (ceiling-floor-label scope) " " (inc row))
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

(defn tf-on-scope-index-select [{:keys [level-key scope index]}]
  (rf/dispatch [::event/set-twt-entry-index level-key scope index]))

(defn tf-twt-entry-partial [{:keys [level-key]
                             {:keys [width height]} :view-size}]
  (let [wl (* width 0.6), wr (- width wl)
        scope @(rf/subscribe [::subs/twt-entry-scope level-key])
        config @(rf/subscribe [::subs/config])
        index @(rf/subscribe [::subs/twt-entry-index scope])]
    [:span
     [:div {:style {:width wl, :height height
                    :display "inline-block"
                    :padding "10px"
                    :vertical-align "top"}}
      (let [k  (case level-key
                 :top :ceiling
                 :bottom :floor
                 nil)]
        [twt-selector {:width (- wl 20), :height (- height 20)
                       :wall-names (get-in config [:tf-config :wall-names])
                       :tube-row-names (map :name (get-in config [:tf-config :tube-rows]))
                       :tube-side-row-key k
                       :tube-side-row-label (ceiling-floor-label k)
                       :level-key level-key
                       :selected {:scope scope, :index index}
                       :on-select tf-on-scope-index-select}])]
     [:div {:style {:width wr, :height height
                    :display "inline-block"
                    :vertical-align "top"}}
      [app-comp/icon-button-l
       {:icon      ic/nav-left
        :disabled? @(rf/subscribe [::subs/twt-entry-nav-disabled? scope :prev])
        :on-click  #(rf/dispatch [::event/move-twt-entry-index scope :prev])}]
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

(defn sf-twt-entry-chamber-tubes [_ chamber ch-index last?]
  (let [{:keys [name start-tube end-tube]} chamber
        path [:side-fired :chambers ch-index]
        pref-fn (fn [index]
                  (get @(rf/subscribe [::subs/tube-prefs ch-index]) index))
        field-fn (fn [index side]
                   @(rf/subscribe [::subs/field-temp
                                   (conj path :sides side :tubes index :raw-temp)]))
        on-change (fn [index side value]
                    (rf/dispatch [::event/set-temp
                                  (conj path :sides side :tubes index :raw-temp)
                                  value false]))
        on-clear #(rf/dispatch [::event/clear-raw-temps path])]
    (fn [height _ _ _]
      [:span
       [list-tube-both-sides
        {:label name
         :height height
         :start-tube start-tube
         :end-tube end-tube
         :on-clear (if @(rf/subscribe [::subs/has-raw-temp path])
                     on-clear)
         :field-fn field-fn
         :pref-fn pref-fn
         :on-change on-change}]])))

(defn sf-twt-entry-side-label [height ch-name side-name]
  [:span {:style {:font-size "12px"
                  :color (ht.style/color-hex :royal-blue)}}
   [:span {:style {:font-weight 300}}
    (translate [:common :reformer :chamber] "Chamber")] ": "
   ch-name
   [:br]
   [:span {:style {:font-weight 300}}
    (translate [:common :reformer :side] "Side")] ": "
   side-name])

(defn sf-twt-entry-fixed-side-label [height state]
  (let [{:keys [ch-index config side]} @state]
    [:div {:style {:height height, :width 160
                   :display "inline-block"
                   :vertical-align "top"}}
     [sf-twt-entry-side-label height
      (get-in config [:sf-config :chambers ch-index :name])
      (get-in config [:sf-config :chambers ch-index :side-names side])]]))

(defn sf-twt-entry-peep-door [_ ch-index side pd-index last?]
  (let [path [:side-fired :chambers ch-index :sides side :wall-temps pd-index :temps]
        field-fn #(deref (rf/subscribe [::subs/field-temp (conj path %)]))
        on-change (fn [index value]
                    (rf/dispatch [::event/set-temp (conj path index) value false]))
        on-clear #(rf/dispatch [::event/clear-wall-temps path])
        on-add #(rf/dispatch [::event/add-temp-field path])
        label  (str (translate [:terms :reformer :peep-door] "Peep door")
                    " " (inc pd-index))]
    (fn [height _ _ _ _ _]
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

(defn sf-twt-entry [{{:keys [width height]} :view-size}]
  ;; the peep door wall temps are shown as a set of columns
  ;; one peep door in each column
  ;; a set of columns for each side of each chamber
  ;; one additional column before each side to show the chamber and side names
  ;; the first one is kept fixed and as the columns are scrolled
  ;; the first column is updated to show the name of the chamber
  ;; and side to which the first visible column belongs
  (let [config @(rf/subscribe [::subs/config])
        state (r/atom {:ch-index 0, :side 0, :config config})
        ch-count (count (get-in config [:sf-config :chambers]))
        pd-count (get-in config [:sf-config :chambers 0 :peep-door-count])
        scope @(rf/subscribe [::subs/twt-entry-scope :reformer])
        side-count (* ch-count 2)
        item-count (case scope
                     :tube ch-count
                     :wall (dec (* (inc pd-count) side-count)))
        last? #(= (inc %) item-count)
        item-width (+ 20 (case scope
                           :tube 220
                           :wall 160))
        render-fn (let [height (- height 20)]
                    (case scope
                      :tube
                      (fn [indexes _]
                        (map (fn [chi]
                               [sf-twt-entry-chamber-tubes
                                height
                                (get-in config [:sf-config :chambers chi])
                                chi (last? chi)])
                             indexes))
                      :wall
                      (fn [indexes _]
                        ;; determin the chamber and side index for the
                        ;; first visible column and update in state
                        (let [i (inc (first indexes))
                              si (quot i (inc pd-count))
                              chi (quot si 2)
                              si (mod si 2)]
                          (swap! state assoc :ch-index chi :side si))
                        ;; return the hiccups for the visible columns
                        ;; note label column in-between set of columns
                        (map (fn [index]
                               (let [i (inc index)
                                     pdi (mod i (inc pd-count))
                                     si (quot i (inc pd-count))
                                     chi (quot si 2)
                                     si (mod si 2)]
                                 (if (zero? pdi)
                                   ;; side label
                                   [sf-twt-entry-side-label
                                    height
                                    (get-in config [:sf-config :chambers chi :name])
                                    (get-in config [:sf-config :chambers chi
                                                    :side-names si])]
                                   ;; peep door temps
                                   [sf-twt-entry-peep-door
                                    height chi si (dec pdi) (last? index)])))
                             indexes))))]
    [:span
     (if (= scope :wall)
       [sf-twt-entry-fixed-side-label height state])
     [:div {:style {:vertical-align "top"
                    :display "inline-block"}}
      [lazy-cols {:width (if (= scope :wall) (- width 160) width)
                  :height height
                  :item-width item-width
                  :item-count item-count
                  :items-render-fn render-fn}]]]))

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
        {:icon (if (= mode :full) ic/magnify ic/dataset)
         :tooltip (case mode
                    :partial (translate [:twt-entry :switch-to-full :tooltip]
                                        "switch to full view")
                    (translate [:twt-entry :switch-to-partial :tooltip]
                               "switch to partial view"))
         :on-click #(rf/dispatch [::event/set-twt-entry-mode
                                  (if (= mode :partial) :full :partial)])}])]))

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
