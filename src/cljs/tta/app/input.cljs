(ns tta.app.input
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [re-frame.core :as rf]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.util.interop :as i]
            [ht.app.subs :refer [translate]]
            [ht.app.style :as ht-style]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [tta.app.style :as app-style]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.scroll :as app-scroll]
            [clojure.string :as str]))

(defn- shift-focus
  "move focus to next input element based on the arrow key pressed.  
  input elements are arranged in rows and cols.  
  **row** : the current row index  
  **col** : the current col index  
  **event** : the DOM event"
  [state row col event]
  (let [key-code (i/oget event :keyCode)
        ;; determine based on key code, whether it is an arrow key
        ;; and the indices of the next input element
        ;; and the row index to scroll into visible area, if requred
        [arrow? row col row-to-show]
        (case key-code
          37 [true row (dec col) row]       ;; left
          38 [true (dec row) col (- row 2)] ;; up
          39 [true row (inc col) row]       ;; right
          40 [true (inc row) col (+ row 2)] ;; down
          ;; ignore other keys
          [false])]
    (when arrow?
      ;; better to eliminate any other DOM action, since we are handling it here
      (doto event
        (i/ocall :preventDefault)
        (i/ocall :stopPropagation))
      ;; do the focus shift when the next col indices are valid
      (let [{:keys [show-row input-elements], [rn cn] :counts} @state]
        (when (and (> rn row -1) (> cn col -1))
          (show-row row-to-show)
          ;; do the focus if input element already mounted
          ;; else store the focus target in state so as to auto foucs on mount
          (if-let [input (get-in input-elements [row col])]
            (i/ocall input :focus)
            (swap! state assoc :focus [row col])))))))

(defn- register-input
  "store the input element in **state** so as to focus it when required.
  and if already marked for focus, then do the focus right away.
  provide *nil* for **input** to clear one already registered."
  [state row col input]
  (let [{:keys [focus]} (swap! state assoc-in [:input-elements row col] input)]
    (when (and input
               (= [row col] focus))
      (i/ocall input :focus)
      (swap! state assoc :focus nil))))

(defn- on-focus-input
  "event handler to auto select the content of input on focus"
  [event]
  (-> (i/oget event :target)
      (i/ocall :select)))

(defn- clipboard-data
  "helper to retrieve clipboard content in most browsers"
  [event]
  (if-let [c (i/oget event :clipboardData)]
    (i/ocall c :getData "text/plain")
    (if-let [c (i/oget-in event [:originalEvent :clipboardData])]
      (i/ocall c :getData "text/plain")
      (if-let [c (i/oget js/window :clipboardData)]
        (i/ocall c :getData "Text")))))

(defn- on-change-input
  "event handler to handle editing of one of input elements arranged
  in rows and columns."
  [state row col event]
  (let [{:keys [on-change]} (:props @state)
        v (i/oget-in event [:target :value])]
    (on-change row col (not-empty v))))

(defn- on-paste-input
  "paste event handler to split paste content using new-lines and tabs into a
  rows and columns of texts which will be then set to the input elements
  arranged in rows and columns."
  [state row col event]
  ;; better to prevent default paste behavior as we are overriding it.
  (doto event
    (i/ocall :preventDefault)
    (i/ocall :stopPropagation))
  ;; to ensure uniform behavior whether single or multiple are present or any
  ;; empty parts are there, here we ensure a separator (\n or \t) in the end
  ;; followed by some text (.), and then split and take all but the last one.
  ;; this is needed because the clojure.string/split tends to ignore multiple
  ;; consecutive separators at the end of the text if there is no
  ;; other text after it.
  (if-let [txt (not-empty (clipboard-data event))]
    (let [txt (str/replace-first txt #"\n?$" "\n.")
          {{:keys [on-change on-paste]} :props, [rn cn] :counts} @state
          ;; on-paste behaves same as on-change if not specified otherwise
          on-paste (or on-paste on-change)]
      (doseq [[l i] (map list (butlast (str/split txt #"\r?\n")) (range))]
        (doseq [[c j] (map list (butlast (str/split (str l "\t.") #"\t")) (range))]
          (let [row (+ row i)
                col (+ col j)]
            (if (and (> rn row) (> cn col))
              (on-paste row col (not-empty c)))))))))

;; *x48
(defn- list-head [width label on-clear]
  [app-comp/action-label-box {:width (- width 44) ;; icon 24 & padding takes 44
                              :label label
                              :right-icon ic/delete
                              :right-action on-clear
                              :right-disabled? (not on-clear)}])

;; 68x30
(defn- list-input [state row col _ _]
  (let [on-paste (partial on-paste-input state row col)
        on-change (partial on-change-input state row col)
        on-key-down (partial shift-focus state row col)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (register-input state row col (dom/dom-node this)))
      :component-will-unmount
      (fn [_]
        (register-input state row col nil))
      :reagent-render
      (fn [_ _ _ style field]
        (let [{:keys [value valid?]} field]
          [:input (merge (use-sub-style style
                                        (if valid? :input :invalid-input))
                         {:value (or value "")
                          :on-focus on-focus-input
                          :on-paste on-paste
                          :on-change on-change
                          :on-key-down on-key-down})]))})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 220x*
(defn- list-tubes
  "!!! should not be used directly !!!  
  render list of tubes using specified renderer for each tube.  
  [width row-height row-render-fn props  
   sub-head props-fn]  
  **on-clear**: (fn []) should clear out all, provide nil to disable button"
  [width row-height row-render-fn props
   ;; additional parameters
   sub-head props-fn]
  ;; uses lazy-rows
  ;; for each item, renders using list-row-tube-both-sides
  (let [state (atom {}) ;; props, tube-number-fn, show-row, counts
        items-render-fn (fn [indexes show-row]
                          (swap! state assoc :show-row show-row)
                          (map #(vector row-render-fn state %) indexes))]
    (fn [props]
      (let [props (if props-fn (props-fn props) props)
            {:keys [label height start-tube end-tube on-clear]} props
            w (- width 12) ;; content width
            [tube-count tube-number-fn]
            (if (> end-tube start-tube)
              [(- end-tube (dec start-tube))
               #(+ start-tube %)]
              [(- start-tube (dec end-tube))
               #(- start-tube %)])]
        (swap! state assoc :props props
               :counts [tube-count (or (:field-count props) 2)]
               :tube-number-fn tube-number-fn)
        [:div {:style {:width width, :height height
                       :display "inline-block"
                       :vertical-align "top"}}
         [list-head w label on-clear]
         (if sub-head [sub-head w props])
         [app-scroll/lazy-rows
          {:width w :height (- height 48) ;; leave 48 for list-head
           :item-count tube-count
           :item-height row-height
           :items-render-fn items-render-fn}]]))))

;; LIST-TUBE-BOTH-SIDES ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 208x38
(defn- list-row-tube-both-sides
  "for a tube of given index, a row with 2 inputs, one for each side of the tube"
  [state index]
  (let [{:keys [tube-number-fn], {:keys [field-fn pref-fn]} :props} @state
        style (app-style/tube-list-row (pref-fn index))
        left-field (field-fn index 0)
        right-field (field-fn index 1)
        label-style-key (if (or (:value left-field) (:value right-field))
                          :filled :label)]
    [:span (use-style style)
     [list-input state index 0 style left-field]
     [:span (use-sub-style style label-style-key) (tube-number-fn index)]
     [list-input state index 1 style right-field]]))


;; 220x(48+i38)
(defn list-tube-both-sides
  "can be for temperature input or emissivity input  
  **field-fn**: (fn [index side]) should return the form field for the tube/side  
  **pref-fn**: (fn [index]), should return \\\"imp\\\" or \\\"pin\\\" or nil  
  **on-clear**: (fn []) should clear out all, provide nil to disable button  
  **on-change**: (fn [index side value]) to update value, where: side = 0 or 1  
  **on-paste**: (fn [index side value]) to update value, where: side = 0 or 1,  
  if on-paste is nil, on-change is used instead."
  [{:keys [label height start-tube end-tube on-clear
           field-fn pref-fn on-change on-paste]
    :as props}]
  (list-tubes 220 38 list-row-tube-both-sides props nil nil))

;; LIST-TUBE-PREFS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; 300x(48+i38)
(defn- list-row-tube-prefs
  "for a tube of given index, a selector to choose preference"
  [state index]
  (let [{:keys [tube-number-fn], {:keys [selected-fn on-select]} :props} @state
        pref (selected-fn index)
        style (app-style/tube-list-row pref)
        options [{:id nil, :label "None"}
                 {:id "imp", :label "Important"}
                 {:id "pin", :label "Pinched"}]
        selected (some #(if (= (:id %) pref) %) options)]
    [:span (use-style style)
     [:span (-> (use-sub-style style :filled)
                (update :style assoc :margin "8px 0"))
      (tube-number-fn index)]
     [app-comp/selector {:options options
                         :item-width 70
                         :label-fn :label
                         :value-fn :id
                         :on-select #(on-select index (:id %))
                         :selected selected}]]))

;; 300x(48+i38)
(defn list-tube-prefs
  "for selecting tube preference  
  **selected-fn**: (fn [index]) should return the pref for the tube  
  **on-clear**: (fn []) should clear out all, provide nil to disable button  
  **on-select**: (fn [index selection]) to update preference"
  [{:keys [label height start-tube end-tube on-clear
           selected-fn on-select]
    :as props}]
  (list-tubes 310 48 list-row-tube-prefs props nil nil))

;; LIST-TUBE-VIEW-FACTOR ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- list-sub-head-view-factors [width {:keys [level-key]}]
  (let [wall (translate [:config :wall :label] "Wall")
        ceiling (translate [:config :ceiling :label] "Ceiling")
        floor (translate [:config :floor :label] "Floor")
        names (case level-key
                :top [wall ceiling nil ceiling wall]
                :bottom [wall floor nil floor wall]
                :middle [wall nil wall])]
    [:div {:style {:height 10
                   :padding "0 12px"
                   :display "inline-block"
                   :font-size "10px", :line-height "10px"
                   :color (color-hex :sky-blue)}}
     (doall
      (map-indexed (fn [i t]
                     ^{:key i}
                     [:span {:style {:width (if t 68 30)
                                     :height 10
                                     :margin "0 8px"
                                     :text-align "center"
                                     :display "inline-block"}}
                      t])
                   names))]))

(defn- list-row-tube-view-factors
  "for a tube of given index, a row with 2 inputs, one for each side of the tube"
  [state index]
  (let [{:keys [tube-number-fn]
         {:keys [field-fn pref-fn field-count]} :props} @state
        style (app-style/tube-list-row-view-factors (pref-fn index))
        label-style-key (if (some #(:value (field-fn index %))
                                  (range field-count))
                          :filled :label)]
    [:span (use-style style)
     (doall (map (fn [i]
                   ^{:key i}
                   [list-input state index i style (field-fn index i)])
                 (range (/ field-count 2))))
     [:span (use-sub-style style label-style-key) (tube-number-fn index)]
     (doall (map (fn [i]
                   ^{:key i}
                   [list-input state index i style (field-fn index i)])
                 (range (/ field-count 2) field-count)))]))

(defn list-tube-view-factors
  "For view factor
  *wall-type* is one of :wall, :ceiling or :floor, while
  *side* is 0 or 1, and *index* is the tube index  
  **field-fn**: (fn [index side wall-type]) should return the form field  
  **pref-fn**: (fn [index]), should return \\\"imp\\\" or \\\"pin\\\" or nil  
  **on-clear**: (fn []) should clear out all, provide nil to disable button  
  **on-change**: (fn [index side wall-type value]) to update value  
  **on-paste**: (fn [index side wall-type value]) to update value  
  if on-paste is nil, on-change is used instead."
  [{:keys [level-key
           label height start-tube end-tube
           field-fn pref-fn on-change on-paste on-clear]
    :as props}]
  (let [field-count (if (= level-key :middle) 2 4)
        w (+ 10 24 46 (* field-count 86))
        ;; helper fn to resolve side-index and wall-type from field-index
        f-i-fn (fn [i] (case level-key
                        :middle [:wall i]
                        [(if (< 0 i 3)
                           (case level-key :top :ceiling, :floor)
                           :wall)
                         (get {0 0, 1 0, 2 1, 3 1} i)]))
        f-field-fn (fn [f]
                     (fn [ti fi]
                       (let [[wt si] (f-i-fn fi)]
                         (f ti si wt))))
        f-on-change (fn [f]
                      (if f
                        (fn [ti fi v]
                          (let [[wt si] (f-i-fn fi)]
                            (f ti si wt v)))))]
    (list-tubes w 38 list-row-tube-view-factors props
                ;; additional parameters
                list-sub-head-view-factors
                #(-> %
                     (assoc :field-count field-count)
                     (update :field-fn f-field-fn)
                     (update :on-change f-on-change)
                     (update :on-paste f-on-change)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; synthetic event to simulate key down on add row, to shift focus
(def ^:private key-down-event
  #js{:keyCode 40 ;; down arrow
      :stopPropagation (fn [])
      :preventDefault (fn [])})

(defn- list-row-add-button [state style]
  (let [{:keys [on-add wall-count]} (:props @state)
        on-click (fn []
                   (on-add)
                   (js/setTimeout #(shift-focus state (dec wall-count) 0
                                                key-down-event)
                                  100))]
    [:div (-> (use-sub-style style :add-btn)
              (assoc :on-click on-click))
     [ic/plus (use-sub-style style :add-icon)]
     [:span (use-sub-style style :add-label)
      (translate [:action :add :label] "Add")]]))

;; 126x38
(defn- list-row-wall-temps [state wall-count index]
  (let [{:keys [field-fn]} (:props @state)
        style (app-style/tube-list-row)
        field (field-fn index)]
    [:span (-> (use-style style)
               (update :style assoc :padding-left 20))
     (if (= index wall-count)
       [list-row-add-button state style]
       [list-input state index 0 style field])]))

;; 160x(48+i38)
(defn list-wall-temps
  "[{:keys [label height wall-count on-clear
            field-fn on-add on-change on-paste]}]"
  [props]
  (let [state (atom {}) ;; props, counts, show-row
        items-render-fn (fn [indexes show-row]
                          (let [wc (get-in (swap! state assoc :show-row show-row)
                                           [:props :wall-count])]
                            (map #(vector list-row-wall-temps state wc %)
                                 indexes)))]
    ;; Form-2 render fn
    (fn [{:keys [label height wall-count on-clear] :as props}]
      (swap! state assoc
             ;; ensure the arity of on-change and on-paste
             ;; since col is not applicable here
             :props (-> props
                        (update :on-change #(fn [row _ val] (% row val)))
                        (update :on-paste #(if %
                                             (fn [row _ val] (% row val)))))
             :counts [wall-count 1])
      (let [width 160, w (- width 12)]
        [:div {:style {:width width, :height height
                       :display "inline-block"
                       :vertical-align "top"}}
         [list-head w label on-clear]
         [app-scroll/lazy-rows
          {:width w, :height (- height 48)
           :item-count (inc wall-count) ;; one row extra for add btn
           :item-height 38
           :items-render-fn items-render-fn}]]))))

;;; top-fired burners ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- tf-burner-input [state row col _]
  (let [bw 80, tw 30
        {:keys [field-fn color-fn read-only? burner-first?]} (:props @state)
        ml (if (pos? col)
             (+ tw bw -56)
             (if burner-first?
               (+ bw -56 -12)
               (+ tw bw -56 -12)))
        on-paste (partial on-paste-input state row col)
        on-change (partial on-change-input state row col)
        on-key-down (partial shift-focus state row col)]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (register-input state row col (dom/dom-node this)))
      :component-will-unmount
      (fn [_]
        (register-input state row col nil))
      :reagent-render
      (fn [_ _ _ style]
        (let [{:keys [value]} (field-fn row col)
              bg-color (if (and value color-fn) (color-fn value))]
          [:input (merge (update (use-sub-style style :input) :style assoc
                                 :background-color bg-color
                                 :margin-left ml)
                         {:value       (or value "")
                          :on-focus    on-focus-input
                          :on-paste    on-paste
                          :on-change   on-change
                          :on-key-down on-key-down
                          :read-only  read-only?})]))})))

(defn- list-row-tf-burners [state index]
  (let [{{:keys [row-count]} :props, :keys [item-height]} @state
        style (app-style/tf-burner-list-row item-height)]
    [:span (use-style style)
     [:span (use-sub-style style :label) (inc index)]
     (doall (map (fn [i]
                   ^{:key i}
                   [tf-burner-input state index i style])
                 (range row-count)))]))

(defn list-tf-burners
  "[{:keys [height item-count row-count burner-first? read-only?
            color-fn field-fn on-change on-paste]}]"
  [{:keys [item-count row-count burner-first?]}]
  (let [state (atom {}) ;; props, counts, show-row
        bw 80, tw 30, p 40
        w (+ p (* row-count bw)
             (* tw (if burner-first? (dec row-count) (inc row-count))))
        items-render-fn (fn [indexes show-row]
                          (swap! state assoc :show-row show-row)
                          (map #(vector list-row-tf-burners state %) indexes))]
    ;; Form-2 render fn
    (fn [{:keys [height] :as props}]
      (let [item-height (max 38 (/ height item-count))]
        (swap! state assoc
               :props props
               :item-height item-height
               :counts [item-count row-count])
        [app-scroll/lazy-rows
         {:width w, :height height
          :item-count item-count
          :item-height item-height
          :items-render-fn items-render-fn}]))))
