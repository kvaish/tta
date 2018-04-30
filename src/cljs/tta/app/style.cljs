(ns tta.app.style
  (:require [stylefy.core :as stylefy]
            [garden.units :refer [px]]
            [garden.color :as gc]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]))

(defn content-height [view-size]
  (let [{:keys [head-row-height sub-head-row-height]} ht/root-layout]
    (- (:height view-size)
       head-row-height
       sub-head-row-height)))

(def head-content-height 72)

(defn content-body-size [view-size]
  (let [h (content-height view-size)]
    {:height (- h head-content-height 20)
     :width (- (:width view-size) 40)}))

(defonce do-once
  (do
    (stylefy/class "ht-ic-fill" {:fill "currentColor"})

    (stylefy/class "ht-ic-icon" {:fill "none"
                                 :stroke "currentColor"
                                 :stroke-width 1
                                 :display "inline-block"
                                 :user-select "none"
                                 :width "24px", :height "24px"})))

(def widget-transition {:std "450ms ease-in-out"})

(def widget-bg-d (color-hex :alumina-grey 30))
(def widget-bg-e (color-hex :sky-blue))
(def widget-bg-h (color-hex :sky-blue 20))
(def widget-fg (color-hex :white))
(def widget-err (color-hex :red))
(def widget-imp (color-hex :monet-pink))

(def popover
  {:border-radius "8px !important"
   :left "-99999px" ;; to hide initial flashing
   :margin-top "8px !important"
   :overflow-y "visible !important"
   :box-shadow "0 0 16px 4px rgba(0,0,0,0.24), 0 0 2px 2px rgba(0,0,0,0.12) !important"
   ::stylefy/mode {:before {:content "no-close-quote"
                            :height "12px", :width "12px"
                            ;; :border-style "solid"
                            ;; :border-width "6px"
                            ;; :border-color "white"; "white white transparent transparent"
                            :background-color "white"
                            :position "absolute"
                            :top 0, :right "16px"
                            :transform-origin "0 0"
                            :transform "rotate(-45deg)"
                            :box-shadow "4px -4px 16px 0 rgba(0,0,0,0.24), 2px -2px 2px 0 rgba(0,0,0,0.12)"}
                   :after {:content "no-close-quote"
                           :height "8px", :width "24px"
                           :position "absolute"
                           :top 0, :right "8px"
                           :background "white"}}})

;; 72x48
(defn toggle [on? disabled?]
  (let [widget-bg (if disabled? widget-bg-d widget-bg-e)]
    {:display "inline-block"
     :padding "13px 12px"
     :vertical-align "top"
     :width "72px", :height "48px"
     ::stylefy/sub-styles
     {:main {:cursor (if-not disabled? "pointer")
                  :width "48px", :height "22px"
                  :border (str "1px solid " widget-bg)
                  :border-radius "11px"
                  :position "relative"
                  :background (if on? widget-bg widget-fg)
                  :color (if on? widget-fg widget-bg)
                  :transition (:std widget-transition)}
      :label {:display "block"
              :user-select "none"
              :font-size "12px"
              :font-weight 300
              :margin "0 10px 0 10px"
              :text-align (if on? "left" "right")}
      :circle {:border-radius "50%"
               :width "14px", :height "14px"
               :border (if-not on? (str "1px solid " widget-bg))
               :position "absolute"
               :top "3px"
               :background widget-fg
               :left (if on? "29px" "4px")
               :transition (:std widget-transition)}}}))

;; 48x48, icon: 24x24
(defn icon-button [disabled?]
  {:border-radius "50%"
   :background (if disabled? widget-bg-d widget-bg-e)
   :color widget-fg})

;; *x48, icon: 24x24
(defn button [disabled?]
  {:bg (if disabled? widget-bg-d widget-bg-e)
   :fg widget-fg
   :hc widget-bg-h
   :container {:display "inline-block"
               :height "48px"
               :padding "8px 12px"}
   :btn {:border-radius "16px"
         :height "32px"
         :color widget-fg}
   :div {:height "24px"
         :padding "0 24px 0 12px"
         :color widget-fg}
   :icon {:color widget-fg}
   :span {:display "inline-block"
          :vertical-align "top"
          :height "24px"
          :line-height "24px"
          :font-size "12px"
          :margin-left "12px"
          :color widget-fg}})

;; *x48
(defn selector [disabled? valid?]
  (let [widget-bg (if disabled? widget-bg-d widget-bg-e)
        label {:position "absolute"
               :display "inline-block"
               :user-select "none"
               :overflow "hidden"
               :font-size "12px"
               :height "24px"
               :line-height "24px"
               ;; :font-weight 300
               :text-align "center"
               :top "3px"
               :transition (:std widget-transition)
               :color widget-bg}]
    {:display "inline-block"
     :height "48px"
     :padding "8px 12px"
     :vertical-align "top"
     ::stylefy/sub-styles
     {:main {:border (str "1px solid " (if valid? widget-bg widget-err))
             :height "32px"
             :border-radius "16px", :min-width "32px"
             :position "relative"}
      :marker {:background widget-bg
               :height "24px"
               :border-radius "12px", :min-width "24px"
               :transition (:std widget-transition)
               :position "absolute"
               :top "3px"}
      :label (if disabled? label (assoc label :cursor "pointer"))
      :active-label (assoc label :color widget-fg)}}))

;; (120+)*x48
(defn text-input [read-only? valid?]
  {:display "inline-block"
   :padding "8px 12px"
   :vertical-align "top"
   ::stylefy/sub-styles
   {:main {:height "32px"
           :border (str "1px solid " (if valid? widget-bg-e widget-err))
           :border-radius "16px"
           :min-width "32px"
           :padding "0 12px"
           :font-size "12px"
           :color (if valid? widget-bg-e widget-err)
           :background-color widget-fg}}})

;; (120+)*x48
(defn text-area [read-only? valid?]
  {:display "inline-block"
   :padding "8px 12px"
   :vertical-align "top"
   ::stylefy/sub-styles
   {:main {:border (str "1px solid " (if valid? widget-bg-e widget-err))
           :border-radius "16px"
           :resize "none"
           :padding "12px 12px"
           :font-size "12px"
           :color (if valid? widget-bg-e widget-err)
           :background-color widget-fg}}})

;; (120+)x48
(defn action-label-box [left-disabled? right-disabled?]
  (let [widget-bg widget-bg-e
        icon-fg-d (color-hex :sky-blue 30)
        icon-d {:vertical-align "top"
                :border-radius "50%"
                :color icon-fg-d
                :background widget-bg-e
                :border (str "1px solid " icon-fg-d)}
        icon-e (merge icon-d {:cursor "pointer"
                              ;; :background widget-bg-e
                              :border (str "1px solid " widget-fg)
                              :color widget-fg
                              ::stylefy/mode
                              {:hover {:background widget-bg-h}}})]
    {:display "inline-block"
     :padding "8px 12px"
     :vertical-align "top"
     ::stylefy/sub-styles
     {:main {:height "32px"
             :border-radius "16px"
             :padding "4px 4px"
             :background-color widget-bg}
      :span {:display "inline-block"
             :user-select "none"
             :overflow "hidden"
             :vertical-align "top"
             :font-size "12px"
             :color widget-fg
             :height "24px"
             :padding "0 12px"
             :line-height "24px"
             :min-width "62px"}
      :left (if left-disabled? icon-d icon-e)
      :right (if right-disabled? icon-d icon-e)}}))

;; (120+)x48
(defn action-input-box [disabled? valid? action? left-disabled? right-disabled?]
  (let [widget-bg (if disabled? widget-bg-d widget-bg-e)
        left-disabled? (or disabled? left-disabled?)
        right-disabled? (or disabled? right-disabled?)
        icon-d {:vertical-align "top"
                :border-radius "50%"
                :color widget-fg
                :background widget-bg-d}
        icon-e (merge icon-d {:cursor "pointer"
                              :background widget-bg-e
                              ::stylefy/mode
                              {:hover {:background widget-bg-h}}})]
    {:display "inline-block"
     :padding "8px 12px"
     :vertical-align "top"
     ::stylefy/sub-styles
     {:main {:height "32px"
             :border (str "1px solid " (if valid? widget-bg widget-err))
             :border-radius "16px"
             :padding "3px 4px"
             :background-color widget-fg}
      :span {:display "inline-block"
             :user-select "none"
             :overflow "hidden"
             :vertical-align "top"
             :font-size "12px"
             :color (if valid? widget-bg widget-err)
             :height "24px"
             :padding "0 12px"
             :line-height "24px"
             :min-width "62px"
             :cursor (if (and action? (not disabled?)) "pointer")}
      :left (if left-disabled? icon-d icon-e)
      :right (if right-disabled? icon-d icon-e)}}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def scroll-bar
  {::stylefy/sub-styles
   {:bar-h {:position "absolute"
            ;; :z-index "9999"
            :cursor "pointer"
            :left "3px"
            :bottom 0
            :height "9px"}
    :bar-v {:position "absolute"
            ;; :z-index "9999"
            :cursor "pointer"
            :top "3px"
            :right 0
            :width "9px"}
    :line-h {:position "absolute"
             :background (color :alumina-grey -20)
             :bottom "4px"
             :left 0
             :height "1px"}
    :line-v {:position "absolute"
             :background (color :alumina-grey -20)
             :right "4px"
             :top 0
             :width "1px"}
    :track-h {:position "absolute"
              :bottom 0
              :height "9px"
              :padding "3px 0"
              :cursor "ew-resize"
              ::stylefy/mode {:hover {:padding "1px 0"}}}
    :track-v {:position "absolute"
              :right 0
              :width "9px"
              :padding "0 3px"
              :cursor "ns-resize"
              ::stylefy/mode {:hover {:padding "0 1px"}}}
    :track {:background (color :sky-blue)
            :border-radius "4px"
            :width "100%"
            :height "100%"}}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn vertical-line [height]
  {:width "20px"
   :display "inline-block"
   :height (px height)
   :vertical-align "top"
   ::stylefy/sub-styles
   {:line {:border-right "none"
           :margin "20px 10px"
           :border-left (str "1px solid " widget-bg-e)
           :width "0"
           :height (px (- height 40))}}})


(defn horizontal-line [width]
  {:width (px width)
   :border (str "1px solid " widget-bg-e)
   :margin "10px 0 0 10px"})


(defn layout-main [view-size]
  (let [w (:width view-size)
        h (content-height view-size)
        {bh :height, bw :width} (content-body-size view-size)]
    {:height (px h)
     :width (px w)
     :background (color :alumina-grey 70)
     ::stylefy/sub-styles
     {:head {:height (px head-content-height)
             :width (px w)
             :display "flex"}
      :head-left {:flex 1
                  :color (color :royal-blue)
                  :padding "14px 20px"
                  :font-weight 700}
      :title {:display "block"
              :user-select "none"
              :font-size "16px"}
      :sub-title {:display "block"
                  :user-select "none"
                  :font-weight 400
                  :font-size "10px"}
      :head-right {:padding "12px 24px"}
      :body {:height (px bh)
             :width (px bw)
             :margin "0 20px 20px 20px"}}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; *x38
(def tube-list-row
  (let [widget-border (color-rgba :sky-blue 0 0.6)
        widget-border-imp (color-rgba :monet-pink 0 0.6)
        widget-selection (color-rgba :sky-blue 0 0.6)
        imp {:border (str "1px solid " widget-border-imp)
             :color widget-imp}
        filled-imp (assoc imp
                          :background widget-imp
                          :color widget-fg)
        pin {:border (str "1px solid " widget-bg-d)
             :color widget-bg-d}
        filled-pin (assoc pin
                          :background widget-bg-d
                          :color widget-fg)
        tube-label {:display "inline-block"
                    :user-select "none"
                    :border-radius "50%"
                    :border (str "1px solid " widget-border)
                    :font-size "10px"
                    :line-height "26px"
                    ;; :font-weight 300
                    :color widget-bg-e
                    :text-align "center"
                    :vertical-align "top"
                    :width "30px", :height "30px"
                    :margin "0 8px"}
        filled-label (assoc tube-label
                            :color widget-fg
                            :background widget-bg-e)
        labels {nil tube-label
                "imp" (merge tube-label imp)
                "pin" (merge tube-label pin)}
        filled {nil filled-label
                "imp" (merge filled-label filled-imp)
                "pin" (merge filled-label filled-pin)}
        tube-input {:width "68px":height "30px"
                    :border (str "1px solid " widget-border)
                    :border-radius "16px"
                    :padding "0 12px"
                    :text-align "center"
                    :vertical-align "top"
                    :font-size "12px"
                    :color widget-bg-e
                    ::stylefy/mode {:selection {:background widget-selection}}}
        inputs {nil tube-input
                "imp" (merge tube-input imp)
                "pin" (merge tube-input pin)}
        add-btn {:height "30px"
                 :border-radius "16px"
                 :background-color "#54c9e9"
                 :padding "3px 10px"
                 :cursor "pointer"
                 ::stylefy/mode {:hover {:background-color widget-bg-h}}}
        add-icon {:color "#fff"
                  :width "24px"
                  :height "24px"}
        add-label {:color "#fff"
                   :user-select "none"
                   :font-size "12px"
                   :padding "0 1px"
                   :overflow "hidden"
                   :line-height "24px"
                   :height "24px"
                   :vertical-align "top"
                   :display "inline-block"}]
    (fn [pref]
      {:height "38px"
       :display "block"
       :padding "4px 12px"
       ::stylefy/sub-styles
       {:label (labels pref)
        :filled (filled pref)
        :input (inputs pref)
        :invalid-input (assoc tube-input
                              :border (str "1px solid " widget-err)
                              :color widget-err)
        :add-btn add-btn
        :add-icon add-icon
        :add-label add-label}})))

(defn tube-list-row-view-factors [pref]
  (let [style (tube-list-row pref)]
    (update style ::stylefy/sub-styles
            #(let [m {:margin "0 8px"}]
              (-> %
                  (update :input merge m)
                  (update :invalid-input merge m))))))

(defn tab-layout [top-tabs? bot-tabs? width height]
  (let [h2 (- height (if top-tabs? 24 0) (if bot-tabs? 24 0))
        w2 width
        t2 (if top-tabs? 24 0)
        h3 (- h2 40)
        w3 (- width 40)]
    [{:t2 t2, :h2 h2, :w2 w2, :h3 h3, :w3 w3}
     {:position "relative"
      ::stylefy/sub-styles
      {:div2 {:position "absolute", :left 0
              :border (str "1px solid " widget-bg-e)
              :border-top (if top-tabs? "none")
              :background (color-hex :white)
              :border-radius
              (str (if top-tabs? "0 0" "8px 8px")
                   (if bot-tabs? " 8px 0" " 8px 8px"))
              :overflow "hidden"}
       :div3 {:position "absolute"
              :top "20px", :left "20px"
              :background widget-fg
              :overflow "hidden"}}}]))

(defn tab-head [position selected? last?]
  (let [[fg bg] (if selected?
                  [widget-bg-e widget-fg]
                  [widget-fg widget-bg-e])
        b (str "1px solid " widget-bg-e)]
    {:display "inline-block"
     :height (if selected? "26px" "25px") :min-width "88px"
     :padding "0 24px"
     :margin-right (if-not last? "1px")
     :margin-top (if (and (= :bottom position)
                          (not selected?))
                   "1px")
     :margin-bottom (if (and (= :top position)
                          (not selected?))
                   "1px")
     :vertical-align "top"
     :color fg
     :background-color bg
     :border-left b, :border-right b
     :border-top (if (= :top position) b "none")
     :border-bottom (if (= :bottom position) b "none")
     :border-radius (case position
                      :top "8px 8px 0 0"
                      :bottom "0 0 8px 8px")
     :user-select "none"
     :font-size "12px"
     :line-height "24px"
     :text-align "center"
     :cursor (if-not selected? "pointer")
     ::stylefy/mode (if-not selected?
                      {:hover {:background widget-bg-h}})}))
