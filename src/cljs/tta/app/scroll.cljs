(ns tta.app.scroll
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [stylefy.core :refer [use-style use-sub-style]]
            [ht.util.interop :as i]
            [ht.util.common :refer [add-event remove-event
                                    get-control-pos]]
            [tta.util.common :refer [motion spring]]
            [tta.app.style :as app-style]))

(defn scroll-bar [{:keys [h? length page-f pos-f on-scroll]}]
  (let [state (atom {:length length
                     :page-f page-f
                     :pos-f pos-f
                     :on-scroll on-scroll})
        do-drag (fn [e]
                  (let [{:keys [length page-f on-scroll drag?
                                  start-pos-f start-x start-y]} @state]
                    (if (and drag? (fn? on-scroll))
                      (let [{:keys [page-x page-y]} (get-control-pos e)
                            l (if h? (- page-x start-x) (- page-y start-y))
                            p (+ start-pos-f (/ l length (- 1 page-f)))
                            new-pos-f (if (> 0 p) 0
                                          (if (< 1 p) 1
                                              p))]
                        ;; (js/console.log "new pos: " new-pos-f)
                        (on-scroll new-pos-f)))))
        end-drag (fn _ed [e]
                   (let [{:keys [drag? ev-move ev-end]} @state]
                     (when drag?
                       ;; (js/console.log "drag end")
                       (swap! state dissoc :drag? :start-x :start-y :start-pos-f)
                       (remove-event js/window ev-move do-drag)
                       (remove-event js/window ev-end _ed))))
        start-drag (fn [touch? e]
                     (i/ocall e :preventDefault)
                     (i/ocall e :stopPropagation)
                     (let [[ev-move ev-end]
                           (if touch? ["touchmove" "touchend"]
                               ["mousemove" "mouseup"])
                           {:keys [page-x page-y]} (get-control-pos e)]
                       ;; (js/console.log "drag start")
                       (swap! state (fn [s]
                                      (assoc s
                                             :drag? true, :start-pos-f (:pos-f s)
                                             :ev-move ev-move, :ev-end ev-end
                                             :start-x page-x, :start-y page-y)))
                       (add-event js/window ev-move do-drag)
                       (add-event js/window ev-end end-drag)))
        on-click-bar (fn [e]
                       (let [{:keys [length page-f on-scroll]} @state]
                         (if (fn? on-scroll)
                           (let [rect (-> e
                                          (i/oget :target)
                                          (i/ocall :getBoundingClientRect))
                                 pos (if h?
                                       (- (i/oget e :clientX) (i/oget rect :left))
                                       (- (i/oget e :clientY) (i/oget rect :top)))
                                 p (/ (- pos (* length page-f 0.5))
                                      (* length (- 1 page-f)))
                                 new-pos-f (if (> 0  p) 0
                                               (if (< 1 p) 1
                                                   p))]
                             ;; (js/console.log "new pos:" new-pos-f)
                             (on-scroll new-pos-f)))))
        on-click-track #(doto %
                          (i/ocall :preventDefault)
                          (i/ocall :stopPropagation))]

    (fn [{:keys [h? length page-f pos-f on-scroll]
         :or {h? false, pos-f 0}}]
      (let [length (- length 8)
            page-l (* length page-f)
            pos-l (* (- length page-l) pos-f)]
        (swap! state assoc :on-scroll on-scroll
               :length length, :page-f page-f, :pos-f pos-f)
        [:div (-> (use-sub-style app-style/scroll-bar
                                 (if h? :bar-h :bar-v))
                  (update :style merge
                          (if h? {:width length} {:height length}))
                  (assoc :on-click on-click-bar))
         [:div (update (use-sub-style app-style/scroll-bar
                                      (if h? :line-h :line-v))
                       :style merge
                       (if h? {:width length} {:height length}))]
         [:div (-> (use-sub-style app-style/scroll-bar
                                  (if h? :track-h :track-v))
                   (update :style merge
                           (if h?
                             {:left pos-l, :width page-l}
                             {:top pos-l, :height page-l}))
                   (merge {:on-click on-click-track
                           :on-mouse-down (partial start-drag false)
                           :on-touch-start (partial start-drag true)
                           :on-mouse-up end-drag
                           :on-touch-end end-drag}))
          [:div (use-sub-style app-style/scroll-bar :track)]]]))))

(defn- f-> [s p f]
  (if (<= s p) 0 (* f (- s p))))

(defn- f<- [s p o]
  (if (<= s p) 0 (let [f (/ o (- s p))]
                   (if (< f 0) 0
                       (if (> f 1) 1 f)))))

(defn- update-f [sh h t sw w l]
  (let [hf (f<- sh h t)
        t (f-> sh h hf)
        wf (f<- sw w l)
        l (f-> sw w wf)]
    [hf t wf l]))

(defn- update-l [{:keys [sw w] :as state} wf]
  (assoc state :wf wf, :l (f-> sw w wf)))

(defn- update-t [{:keys [sh h] :as state} hf]
  (assoc state :hf hf, :t (f-> sh h hf)))

(defn- update-on-wheel [state e]
  (i/ocall e :preventDefault)
  (let [dx (i/oget e :deltaX)
        dy (i/oget e :deltaY)
        shift? (i/ocall e :getModifierState "Shift")
        [dx dy] (if shift? [dy dx] [dx dy])]
    (swap! state (fn [{:keys [sh h t sw w l] :as state}]
                   (let [[hf t wf l] (update-f sh h (+ t dy)
                                               sw w (+ l dx))]
                     (assoc state :hf hf, :wf wf, :t t, :l l))))))

(defn- scroll-to [state {:keys [top left]}]
  (swap! state (fn [{:keys [sh h sw w] :as state}]
                 (let [[hf t wf l] (update-f sh h top
                                             sw w left)]
                   (assoc state :hf hf, :wf wf, :t t, :l l)))))

(defn- prevent-scroll [e]
  (let [ele (i/oget e :target)]
    (if (pos? (i/oget ele :scrollTop))
      (i/oset ele :scrollTop 0))
    (if (pos? (i/oget ele :scrollLeft))
      (i/oset ele :scrollLeft 0))))

(defn lazy-scroll-box
  "[{:keys [width height scroll-width scroll-height
            style class-name body-style body-class-name render-fn]}]
  **props**: a map with width, height, scroll-width, scroll-height,
  style, class-name, body-style, body-class-name  
  **render-fn**: a function to render the content, it will be passed
  a map with top, left, height, width, scroll-height, scroll-width.
  Also a function is passed in as second parameter to it which can be
  called to scroll it to a specified position. Provide the new position
  as the top left coordinate in the scroll pane like {:top top, :left left}."
  [{:keys [width height scroll-width scroll-height]}]
  ;; (js/console.log "lazy:" [width height scroll-width scroll-height])
  (let [state (r/atom {:wf 0, :hf 0, :l 0, :t 0
                       :h height, :w width
                       :sh scroll-height, :sw scroll-width})
        update-box (fn [state new]
                     (let [state (merge state new)
                           {:keys [sh sw h w t l]} state
                           [hf t wf l] (update-f sh h t sw w l)]
                       (assoc state
                              :hf hf, :wf wf
                              :t t, :l l)))
        scroll-to (partial scroll-to state)]
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [node (dom/dom-node this)]
                               (add-event node "wheel"
                                          (partial update-on-wheel state))))
      :component-will-receive-props (fn [this [_ props]]
                                      ;; (js/console.log "props:" props)
                                      (let [{sh :scroll-height, sw :scroll-width
                                             h :height, w :width} props
                                            old (select-keys @state [:sh :sw :h :w])
                                            new {:sh sh, :sw sw, :h h, :w w}]
                                        (when (not= old new)
                                          ;; (js/console.log "receive-props:" [old new])
                                          (swap! state update-box new))))

      :reagent-render
      (fn [props]
        (let [{:keys [style class-name body-style body-class-name render-fn]} props
              {:keys [sh h hf t, sw w wf l]} @state]
          [:div {:style (assoc style :width w :height h
                               :overflow "hidden"
                               :position "relative")
                 :class-name class-name}
           [:div {:style (assoc style :width w, :height h
                                :overflow "hidden"
                                :position "absolute")
                  :on-scroll prevent-scroll}
            [motion {:defaultStyle #js{:t 0, :l 0}
                     :style #js{:t (spring t), :l (spring l)}}
             (fn [s]
               (let [t (i/oget s :t)
                     l (i/oget s :l)]
                 (r/as-element
                  [:div {:style (assoc body-style
                                       :width sw, :height sh
                                       :position "absolute"
                                       :top (- t), :left (- l))
                         :class-name body-class-name}
                   (if render-fn (render-fn {:top t, :left l
                                             :height h, :scroll-height sh
                                             :width w, :scroll-width sw}
                                            scroll-to))])))]]
           (if (and h sh (< h sh))
             [scroll-bar {:h? false
                          :length h
                          :page-f (/ h sh)
                          :pos-f hf
                          :on-scroll #(swap! state update-t %)}])
           (if (and w sw (< w sw))
             [scroll-bar {:h? true
                          :length w
                          :page-f (/ w sw)
                          :pos-f wf
                          :on-scroll #(swap! state update-l %)}])]))})))

(defn scroll-box [props & children]
  (let [state (r/atom {:wf 0, :hf 0, :l 0, :t 0})
        update-box (fn [state node]
                     (let [bn (or node (:box-node state))
                           {:keys [sh sw t l]} state
                           h (i/oget bn :clientHeight)
                           w (i/oget bn :clientWidth)
                           [hf t wf l] (update-f sh h t sw w l)]
                       ;; (js/console.log "bn: " sh h t hf)
                       (assoc state :box-node bn
                              :h h, :w w
                              :hf hf, :wf wf
                              :t t, :l l)))
        update-scroll (fn [state node]
                        (let [sn (or node (:scroll-node state))
                              {:keys [h w t l]} state
                              sh (i/oget sn :scrollHeight)
                              sw (i/oget sn :scrollWidth)
                              [hf t wf l] (update-f sh h t sw w l)]
                          ;; (js/console.log "sn: " sh h t hf)
                          (assoc state :scroll-node sn
                                 :sh sh, :sw sw
                                 :hf hf, :wf wf
                                 :t t, :l l)))
        changed? (fn [so sn]
                   (let [{sho :sh ho :h hfo :hf} so
                         {shn :sh hn :h hfn :hf} sn]
                     (or (< 0.1 (js/Math.abs (- sho shn)))
                         (< 0.1 (js/Math.abs (- ho hn)))
                         (< 1e-6 (js/Math.abs (- hfo hfn))))))

        ;; inner scroll body
        body (fn [_ children]
               (r/create-class
                {:component-did-mount (fn [this]
                                        ;; (js/console.log "body mounted")
                                        (swap! state update-scroll
                                               (dom/dom-node this)))
                 :component-did-update (fn [_ _]
                                         ;; (js/console.log "body updated")
                                         (let [o @state
                                               n (update-scroll o nil)]
                                           (when (changed? o n)
                                             ;; (js/console.log n)
                                             (swap! state merge n))))
                 :reagent-render
                 (fn [_ children]
                   (let [{:keys [t l]} @state]
                     [motion {:defaultStyle #js{:t 0, :l 0}
                              :style #js{:t (spring t), :l (spring l)}}
                      (fn [s]
                        (let [t (i/oget s :t)
                              l (i/oget s :l)]
                          (r/as-element
                           (into [:div {:style {:position "absolute"
                                                :top (- t) :left (- l)}}]
                                 children))))]))}))]

    ;; outer scroll box
    (r/create-class
     {:component-did-mount (fn [this]
                             (let [node (dom/dom-node this)]
                               ;; (js/console.log "box mounted")
                               ;; TODO: later add a timer based update call
                               ;; to check changes in sizes by flex etc.
                               (add-event node "wheel"
                                          (partial update-on-wheel state))
                               (swap! state update-box node)))
      :component-did-update (fn [_ _]
                              ;; (js/console.log "box updated")
                              (let [o @state
                                    n (update-box o nil)]
                                (when (changed? o n)
                                  ;; (js/console.log n)
                                  (swap! state merge n))))
      :reagent-render
      (fn [{:keys [style] :as props} & children]
        (let [{:keys [sw w wf, sh h hf]} @state]
          ;; (js/console.log "wf:" wf "hf:" hf)
          [:div (assoc props :style (merge style {:overflow "hidden"
                                                  :position "relative"})
                       :on-scroll prevent-scroll)
           [body {} children]
           (if (and h sh (< h sh))
             [scroll-bar {:h? false
                          :length h
                          :page-f (/ h sh)
                          :pos-f hf
                          :on-scroll #(swap! state update-t %)}])
           (if (and w sw (< w sw))
             [scroll-bar {:h? true
                          :length w
                          :page-f (/ w sw)
                          :pos-f wf
                          :on-scroll #(swap! state update-l %)}])]))})))

(defn- show-list-item [state index]
  (let [{:keys [scroll-to top height item-height]} @state
        item-top (* index item-height)
        item-bottom (+ item-top item-height)]
    (if (< item-top top)
      (scroll-to {:top item-top, :left 0})
      (if (> item-bottom (+ top height))
        (scroll-to {:top (- item-bottom height), :left 0})))))

(defn lazy-list-box
  "[{:keys [width height item-count item-height render-items-fn]}]
  Good for showing long list of dom heavy items. It renders only those visible.  
  **render-items-fn**: (fn [index-list show-item])  
  It should return a sequence of hiccups , for each index in **index-list**.
  You can scroll to bring an item into view by calling the **show-item**
  function which takes a single argument *item-index*."
  [props]
  (let [state (atom {})
        render-fn
        (fn [{:keys [top]} scroll-to]
          (let [{:keys [height item-height item-count render-items-fn]}
                (swap! state assoc :top top, :scroll-to scroll-to)
                from (quot top item-height)
                to (min item-count (js/Math.ceil (/ (+ top height) item-height)))
                items (render-items-fn (range from to)
                                       (partial show-list-item state))]
            (doall
             (map (fn [item i]
                    (let [index (+ from i)]
                      [:span {:key index
                              :style {:display "block"
                                      :margin "0", :padding "0"
                                      :position "absolute"
                                      :top (* item-height index)
                                      :left 0}}
                       item]))
                  items (range)))))]
    (fn [{:keys [width height item-count item-height] :as props}]
      (let [scroll-height (* item-height item-count)]
        (swap! state merge props)
        [lazy-scroll-box
         {:width width, :height height
          :scroll-width width, :scroll-height scroll-height
          :render-fn render-fn}]))))

(defn- show-list-col [state index]
  ;;TODO:
  )

(defn- lazy-list-cols
  ""
  [props]
  ;;TODO:
  )

(defn- show-grid-item [state row col]
  ;;TODO:
  )

(defn- lazy-grid-box
  ""
  [props]
  ;;TODO:
  )
