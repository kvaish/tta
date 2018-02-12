(ns tta.app.scroll
  (:require [reagent.core :as r]
            [reagent.dom :as dom]
            [stylefy.core :refer [use-style use-sub-style]]
            [ht.util.interop :as i]
            [ht.util.common :refer [add-event remove-event
                                    get-control-pos]]
            [tta.app.style :as app-style]))

(defn scroll-bar [{:keys [h? length page-f pos-f on-scroll]}]
  (let [state (atom {:length length
                     :page-f page-f
                     :pos-f pos-f
                     :on-scroll on-scroll})
        do-drag (fn [e]
                  (if (:drag? @state)
                    (let [{:keys [length page-f on-scroll
                                  start-pos-f start-x start-y]} @state
                          {:keys [page-x page-y]} (get-control-pos e)
                          l (if h? (- page-x start-x) (- page-y start-y))
                          p (+ start-pos-f (/ l length (- 1 page-f)))
                          new-pos-f (if (> 0 p) 0
                                        (if (< 1 p) 1
                                            p))]
                      ;; (js/console.log "new pos: " new-pos-f)
                      (if (fn? on-scroll) (on-scroll new-pos-f)))))
        end-drag (fn _ed [e]
                   (let [{:keys [drag? ev-move ev-end]} @state]
                     (when drag?
                       ;; (js/console.log "drag end")
                       (swap! state dissoc :drag? :start-x :start-y :start-pos-f)
                       (remove-event js/window ev-move do-drag)
                       (remove-event js/window ev-end _ed))))
        start-drag (fn [touch? e]
                     (i/ocall e :preventDefault)
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
                       (add-event js/window ev-end end-drag)))]
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
                  (assoc :on-click (fn [e] ;; TODO: add click handler
                                     )))
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
                   (merge {:on-mouse-down (partial start-drag false)
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

(defn lazy-scroll-box
  "**props**: a map with width, height, scroll-width, scroll-height,
  style, class-name, body-style, body-class-name  
  **render-fn**: a function to render the content, it will be passed
  a map with top, left, height, width, scroll-height, scroll-width"
  [{:keys [width height scroll-width scroll-height]} render-fn]
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
                              :t t, :l l)))]
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
      (fn [props render-fn]
        (let [{:keys [style class-name body-style body-class-name]} props
              {:keys [sh h hf t, sw w wf l]} @state]
          ;; (js/console.log "props:" props)
          ;; (js/console.log "state:" state)
          ;; (js/console.log "sh sw h w" [sh sw h w])
          [:div {:style (assoc style :width w :height h
                               :overflow "hidden"
                               :position "relative")
                 :class-name class-name}
           [:div {:style (assoc body-style
                                :width sw, :height sh
                                :position "absolute"
                                :transition "200ms ease-in-out"
                                :top (- t), :left (- l))
                  :class-name body-class-name}
            (if render-fn (render-fn {:top t, :left l
                                      :height h, :scroll-height sh
                                      :width w, :scroll-width sw}))]
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
                     (into [:div {:style {:position "absolute"
                                          :transition "200ms ease-in-out"
                                          :top (- t) :left (- l)}}]
                           children)))}))]

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
                                                  :position "relative"}))
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
