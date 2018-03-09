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

(defn- shift-focus [state index side event]
  (let [key-code (i/oget event :keyCode)
        [arrow? index side show-index]
        (case key-code
          37 [true index (dec side) index] ;; left
          38 [true (dec index) side (- index 2)] ;; up
          39 [true index (inc side) index]       ;; right
          40 [true (inc index) side (+ index 2)] ;; down
          [false])]
    (when arrow?
      ;; (js/console.log "arrow" index side)
      (doto event
        (i/ocall :preventDefault)
        (i/ocall :stopPropagation))
      (when (and (>= index 0) (>= 1 side 0))
        (let [{:keys [show-row input]} (swap! state assoc :focus [index side])
              input (get-in input [index side])]
          (show-row show-index)
          (if input (set-focus state input)))))))

(defn- register-input [state index side input]
  (let [{:keys [focus]} (swap! state assoc-in [:input index side] input)]
    (when (and input (= [index side] focus))
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

(defn- on-change-input [state index side event]
  (let [{:keys [on-change]} (:props @state)
        v (i/oget-in event [:target :value])]
    (on-change index side (not-empty v))))

(defn- on-paste-input [state index side event]
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
                side (+ side j)]
            (if (and (> tube-count index) (> 2 side))
              (on-paste index side (not-empty c)))))))))

;; 68x30
(defn- tube-list-input [state index side _ _]
  (let [on-paste (partial on-paste-input state index side)
        on-change (partial on-change-input state index side)
        on-key-down (partial shift-focus state index side)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (register-input state index side (dom/dom-node this)))
      :component-will-unmount
      (fn [_]
        (register-input state index side nil))
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

;; 208x38
(defn- tube-list-row [state index field-fn]
  (let [{:keys [tube-number], {:keys [pref-fn]} :props} @state
        style (app-style/tube-list-row (pref-fn index))
        left-field (field-fn index 0)
        right-field (field-fn index 1)
        label-style-key (if (or (:value left-field) (:value right-field))
                          :filled :label)]
    [:span (use-style style)
     [tube-list-input state index 0 style left-field]
     [:span (use-sub-style style label-style-key) (tube-number index)]
     [tube-list-input state index 1 style right-field]]))

;; 220x48
(defn- tube-list-head [label on-clear]
  [app-comp/action-label-box {:width 164 ;; icon 24 & padding 32 takes 56
                              :label label
                              :right-icon ic/delete
                              :right-action on-clear
                              :right-disabled? (not on-clear)}])

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
         [tube-list-head label on-clear]
         [app-scroll/lazy-list-box
          {:width wl, :height (- height 48)
           :item-count tube-count
           :item-height 38
           :render-items-fn render-items-fn
           :-*- field-fn}]]))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wall-list
  "[{:keys [label height wall-count
            field-fn on-clear on-add on-change on-paste]}]"
  [props]
  ;;TODO: just one input per row, but with one plus button to add more field
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- tube-prefs-head [label width on-clear]
  [app-comp/action-label-box {:width width ;; icon 24 & padding 32 takes 56
                              :label label
                              :right-icon ic/delete
                              :right-action on-clear
                              :right-disabled? (not on-clear)}])

(defn render-tubes [props ch ind]
  [:div {:style {:position "absolute"
                 :left (str (* ind 100) "px")}}

   (let [{:keys [plant on-clear]} props
         firing (get-in  plant [:config :firing])
         label (case firing
                 "side" (get-in plant [:config :sf-config :chambers ind :name])
                 "top" (get-in plant [:config :tf-config :rows ind :name])
                 "default")]
     (tube-prefs-head label (-  (/ (:width props) 2) 100) on-clear))

   (let [{:keys [width height  plant item-height on-select item-width]} props
         tube-prefs (:tube-prefs ch)
         firing (get-in  plant [:config :firing])
         {:keys [start-tube end-tube]} (case firing
                                         "side" (get-in plant [:config :sf-config :chambers ind])
                                         "top" (get-in plant [:config :tf-config :rows ind :name]))
         render-items-fn
         (fn [indexes show-item]
           (map (fn [i]
                  [:div  (merge  (use-style (app-style/tube-list-row))
                                 {:style {:margin-top "10px"
                                          :display "inline-table"
                                          :height item-height
                                          :width (/ width 2)}})  
                   [:span (merge (use-sub-style (app-style/tube-list-row) :label)
                                 {:style {:margin-top "10px"}})  
                    (if (> start-tube end-tube)
                      (- start-tube i)
                      (- end-tube i ))]
                   [:span 
                    [app-comp/selector {:options ["imp" "pinched" "none"]
                                        :item-width 70
                                        :on-select #(on-select i)
                                        :selected (tube-prefs i)}]]])   
                indexes))]

     [app-scroll/lazy-list-box {:height height
                                :width (/ width 2)
                                :item-height item-height
                                :item-count (count tube-prefs)
                                :render-items-fn render-items-fn}])])

(defn tube-pref-list
  [props]
  (let [{:keys [width height item-height item-width
                tube-prefs  on-clear on-select rows]} props
        render-items-fn
        (fn [indexes show-item]
          (map-indexed (fn [ind ch]
                         (render-tubes props ch ind)) rows))] 

    [app-scroll/lazy-list-cols {:height height
                                :width width
                                :item-width item-width  
                                :item-count (count rows)
                                :render-items-fn render-items-fn}]))
