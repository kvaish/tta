(ns tta.app.input
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.util.interop :as i]
            [ht.app.subs :refer [translate]]
            [ht.app.style :as ht-style]
            [tta.app.style :as app-style]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :as app-scroll]
            [clojure.string :as str]))

(defn- set-focus [state input]
  (i/ocall input :focus)
  (swap! state assoc :focus nil))

(defn- shift-focus [state index col-no event]
  (let [key-code (i/oget event :keyCode)
        [arrow? index col-no show-index]
        (case key-code
          37 [true index (dec col-no) index] ;; left
          38 [true (dec index) col-no (- index 2)] ;; up
          39 [true index (inc col-no) index]       ;; right
          40 [true (inc index) col-no (+ index 2)] ;; down
          [false])]
    (when arrow?
      ;; (js/console.log "arrow" index side)
      (doto event
        (i/ocall :preventDefault)
        (i/ocall :stopPropagation))
      (when (and (>= index 0) (>= 1 col-no 0))
        (let [{:keys [show-row input]} (swap! state assoc :focus [index col-no])
              input (get-in input [index col-no])]
          (show-row show-index)
          (if input (set-focus state input)))))))

(defn- register-input [state index col-no input]
  (let [{:keys [focus]} (swap! state assoc-in [:input index col-no] input)]
    (when (and input (= [index col-no] focus))
      (set-focus state input))))

(defn- on-focus-input [event]
  (-> (i/oget event :target)
      (i/ocall :select)))

(defn- clipboard-data [event]
  (if-let [c (i/oget event :clipboardData)]
    (i/ocall c :getData "text/plain")
    (if-let [c (i/oget-in event [:originalEvent :clipboardData])]
      (i/ocall c :getData "text/plain")
      (if-let [c (i/oget js/window :clipboardData)]
        (i/ocall c :getData "Text")))))

(defn- on-change-input [state index col-no event]
  (let [{:keys [on-change]} (:props @state)
        v (i/oget-in event [:target :value])]
    (on-change index col-no (not-empty v))))

(defn- on-paste-input [state index col-no event]
  (doto event
    (i/ocall :preventDefault)
    (i/ocall :stopPropagation))
  (if-let [txt (not-empty (clipboard-data event))]
    (let [txt (str/replace-first txt #"\n?$" "\n.")
          {{:keys [on-change on-paste]} :props
           :keys [tube-count]} @state
          on-paste (or on-paste on-change)]
      (js/console.log (pr-str txt))
      (doseq [[l i] (map list (butlast (str/split txt #"\r?\n")) (range))]
        (doseq [[c j] (map list (butlast (str/split (str l "\t.") #"\t")) (range))]
          (let [index (+ index i)
                side (+ col-no j)]
            (if (and (> tube-count index) (> 2 side))
              (on-paste index side (not-empty c)))))))))

;; 220x48
(defn- list-head [label on-clear width]
  [app-comp/action-label-box {:width width ;; icon 24 & padding 32 takes 56
                              :label label
                              :right-icon ic/delete
                              :right-action on-clear
                              :right-disabled? (not on-clear)}])

;; 68x30
(defn- list-input [state index col-no _ _]
  (let [on-paste (partial on-paste-input state index col-no)
        on-change (partial on-change-input state index col-no)
        on-key-down (partial shift-focus state index col-no)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (register-input state index col-no (dom/dom-node this)))
      :component-will-unmount
      (fn [_]
        (register-input state index col-no nil))
      :reagent-render
      (fn [_ _ _ style field]
        (let [{:keys [value valid?]} field]
          [:input (merge (use-sub-style style
                                        (if valid? :input :invalid-input))
                         {:value (or value "")
                          ;; :ref #(register-input index side %)
                          :on-focus on-focus-input
                          :on-paste on-paste
                          :on-change on-change
                          :on-key-down on-key-down})]))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 208x38
(defn- tube-list-row [state index field-fn]
  (let [{:keys [tube-number], {:keys [pref-fn]} :props} @state
        style (app-style/tube-list-row (pref-fn index))
        left-field (field-fn index 0)
        right-field (field-fn index 1)
        label-style-key (if (or (:value left-field) (:value right-field))
                          :filled :label)]
    [:span (use-style style)
     [list-input state index 0 style left-field]
     [:span (use-sub-style style label-style-key) (tube-number index)]
     [list-input state index 1 style right-field]]))

;; 220x*
(defn tube-list
  "[{:keys [label height
            start-tube end-tube
            field-fn pref-fn
            on-clear on-change on-paste]}]  
  **field-fn**: (fn [index side]) should return the form field for the tube/side  
  **pref-fn**: (fn [index]), should return \"imp\" or \"pin\" or nil  
  **on-clear**: (fn []) should clear out all, provide nil to disable button  
  **on-change**: (fn [index side value]) to update value, where: side = 0 or 1  
  **on-paste**: (fn [index side value]) to update value, where: side = 0 or 1,
  if on-paste is nil, on-change is used instead."
  [props]
  (let [state (atom {}) ;; props, tube-number show-row
        render-items-fn
        (fn [indexes show-row]
          (let [{{:keys [field-fn]} :props} (swap! state assoc :show-row show-row)]
            (map (fn [i] [tube-list-row state i field-fn]) indexes)))]
    (fn [{:keys [height start-tube end-tube label on-clear field-fn] :as props}]
      (let [w 220, wl 208
            [tube-count tube-number]
            (if (> end-tube start-tube)
              [(- end-tube (dec start-tube))
               #(+ start-tube %)]
              [(- start-tube (dec end-tube))
               #(- end-tube %)])]
        (swap! state assoc :props props
               :tube-count tube-count
               :tube-number tube-number)
        [:div {:style {:width w, :height height}}
         [list-head label on-clear 164]
         [app-scroll/lazy-list-box
          {:width wl, :height (- height 48)
           :item-count tube-count
           :item-height 38
           :render-items-fn render-items-fn
           :-*- field-fn}]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 208x38
(defn- wall-list-row [state index n field-fn add-row-fn]
  (let [{:keys [tube-number], {:keys [pref-fn]} :props} @state
        style (app-style/wall-list-row)
        field (field-fn index 0)
        btn-style (app-style/add-row-btn)]

    (if (= index n)

       #_[app-comp/button {:disabled? false
                          :icon      ic/plus
                          :label     (translate [:action :add :label] "Add")
                          :on-click  #(add-row-fn n)}]
       [:span {:style {:display       " inline-block"
                       :padding       " 4px 12px"
                       :vertical-align " top"
                       :cursor "pointer"
                       }
               :on-click  #(add-row-fn n)}
        [:div {:style {:height "32px"
                       :border-radius "16px"
                       :background-color "#54c9e9"
                       :padding "4px 10px"}}
         [ic/plus {:style {:color "#fff"
                           :width "18px"
                           :height "20px"}}]
         [:span {:style {:color "#fff"
                         :font-size "12px"
                         :padding "0 4px"
                         :overflow "hidden"
                         :line-height "24px"
                         :height "24px"
                         :vertical-align "top"
                         :display "inline-block"}}
          (translate [:action :add :label] "Add")]]]

       [:span (use-style style)
       [list-input state index 0 style field]])))

(defn wall-list
  "[{:keys [label height wall-count
            field-fn on-clear on-add on-change on-paste]}]"
  [props]
  (let [state (atom {}) ;; props, tube-number show-row
        render-items-fn
        (fn [indexes show-row]
          (let [{{:keys [field-fn add-row-fn end-tube]} :props} (swap! state assoc :show-row show-row)
                new-indexes (if (zero? (first indexes))
                              indexes
                              (cons (first indexes) (map inc indexes)))]
            (map (fn [i] [wall-list-row state i end-tube field-fn add-row-fn]) new-indexes)))]
    (fn [{:keys [height start-tube end-tube label on-clear field-fn] :as props}]
      (let [w 138, wl 126
            [tube-count tube-number]
            (if (> end-tube start-tube)
              [(inc (- end-tube (dec start-tube)))
               #(+ start-tube %)]
              [(inc (- start-tube (dec end-tube)))
               #(- end-tube %)])]
        (swap! state assoc :props props
               :tube-count tube-count
               :tube-number tube-number)
        [:div {:style {:width w, :height height}}
         [list-head label on-clear 82]
         [app-scroll/lazy-list-box
          {:width wl, :height (- height 48)
           :item-count tube-count
           :item-height 38
           :render-items-fn render-items-fn
           :-*- field-fn}]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tube-pref-list
  "[{:keys [label height
            start-tube end-tube
            field-fn on-clear on-change]}]"
  [props]
  ;;TODO: show tube number and a slider to choose none/important/pinched
  ;; on-clear would set all to none
  )
