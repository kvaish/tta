(ns tta.app.calendar
  (:require [clojure.set :as set]
            [reagent.core :as r]
            [reagent.dom :as dom]
            [stylefy.core :as stylefy]
            [ht.app.subs :refer [translate]]
            [ht.style :as ht-style]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]))


;;;;;;;;;; Styles Start ;;;;;;;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def s-week-header {:display "inline-block"
                              :width "30px"
                              :padding "5px"
                              :font-size "9px"
                              :vertical-align "top"
                              :text-align "center"
                              :color (ht-style/colors :slate-grey)})

(stylefy/class "s-opacity-transition" {:transition "opacity 0.1s"})

(stylefy/class "s-transparent" {:opacity "0"})

(defn- s-date [valid?] {:display "inline-block"
             :width "30px"
             :padding "10px 5px "
             :vertical-align "top"
             :text-align "center"
             :opacity (if valid? 1 0.3)
             :color (ht-style/colors :royal-blue)})

(defn- s-date-dots [background-color] {:margin "auto"
                                       :margin-top "10px"
                                       :width "6px"
                                       :height "6px"
                                       :border-radius "3px"
                                       :background-color background-color})

(defn- s-full-toggle-button [is-selected? enabled?]
  (merge {:background-color (if is-selected?
            (ht-style/colors :sky-blue)
            (ht-style/colors :white))
          :color (if is-selected?
            (ht-style/colors :white)
            (ht-style/colors :sky-blue))
          :text-align "center"
          :opacity (if enabled? 1 0.6)
          :display "inline-block"
          :width "56px"
          :font-size "9px"
          :height "26px"
          :padding-top "6px"
          :border-radius "13px"
          :margin "3px"
          :cursor "pointer"
          ::stylefy/mode
          {:hover (if enabled? {:background-color (ht-style/colors :alumina-grey)
                   :color (ht-style/colors :white)})}}))

(def s-range-options {:border-top "1px solid #CFD2D3"
                      :padding "10px"
                      :background-color "#F1F1F1"})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;;;;;;;; Styles End ;;;;;;;;;;;;


(def range-options
  { :custom {:label "Custom" :to-add nil} 
    :week {:label "1 week" :to-add {:days 7 :months 0 :years 0}}
    :month {:label "1 month" :to-add {:days 0 :months 1 :years 0}}
    :six-months {:label "6 months" :to-add {:days 0 :months 6 :years 0}}
    :year {:label "1 year" :to-add {:days 0 :months 0 :years 1}}})

(defn week-days []
  [(translate [:calendar :week-days :Sunday] "Sunday")
   (translate [:calendar :week-days :Monday] "Monday")
   (translate [:calendar :week-days :Tuesday] "Tuesday")
   (translate [:calendar :week-days :Wednesday] "Wednesday")
   (translate [:calendar :week-days :Thursday] "Thursday")
   (translate [:calendar :week-days :Friday] "Friday")
   (translate [:calendar :week-days :Saturday] "Saturday")])

(defn month-names []
  [(translate [:calendar :month-names :January] "January")
   (translate [:calendar :month-names :February] "February")
   (translate [:calendar :month-names :March] "March")
   (translate [:calendar :month-names :April] "April")
   (translate [:calendar :month-names :May] "May")
   (translate [:calendar :month-names :June] "June")
   (translate [:calendar :month-names :July] "July")
   (translate [:calendar :month-names :August] "August")
   (translate [:calendar :month-names :September] "September")
   (translate [:calendar :month-names :October] "October")
   (translate [:calendar :month-names :November] "November")
   (translate [:calendar :month-names :December] "December")])

(defn- update-values [m f & args]
 (reduce (fn [r [k v]] (assoc r k (apply f v args))) {} m))

(defn- is-leap-year? [year]
  (or (and
        (= 0 (mod year 4))
        (not= 0 (mod year 100)))
      (= 0 (mod year 400))))

(defn- no-of-days
  [year month]
  ([31 
    (if (is-leap-year? year) 29 28) 
    31 30 31 30 31 31 30 31 30 31] (dec month)))

(defn- get-day-of-week [year month day]
  (.getDay (js/Date. year (dec month) day)))

(defn add-days
  ( [{:keys [day month year]} {:keys [days months years]} adjust-fn]
    (let [jsDate (js/Date. year (dec month) day)]
      (.setYear jsDate (+ years (.getFullYear jsDate)))
      (.setMonth jsDate (+ months (.getMonth jsDate)))
      (.setDate jsDate (adjust-fn (+ days (.getDate jsDate))))
      {:day (.getDate jsDate)
      :month (inc (.getMonth jsDate))
      :year (.getFullYear jsDate)}))
  ( [date period]
    (add-days date period identity)))

(defn- dates-to-show [year month week-start]
  (let [full-date (fn [d m y] {:day d :month m :year y})
        pmonthno (if (= month 1) 12 (- month 1))
        pmonth (no-of-days year pmonthno)
        cmonth (no-of-days year month)
        start-day (get-day-of-week year month 1)
        days-pmonth-raw (- start-day week-start)
        days-pmonth-actual (if (< days-pmonth-raw 0)
                             (+ 7 days-pmonth-raw)
                             days-pmonth-raw)
        days-to-show-pc (into []
          (concat
            (map full-date
                (range (->> days-pmonth-actual (- pmonth) (+ 1))
                        (+ pmonth 1))
                (cycle [pmonthno])
                (cycle [(if (= pmonthno 12) (- year 1) year)]))
            (map full-date
                (range 1 (+ 1 cmonth))
                (cycle [month])
                (cycle [year]))))
        all-days (->> (map full-date
                           (->> days-to-show-pc (count) (- 43) (range 1))
                           (cycle [(if (= month 12) 1 (+ 1 month))])
                           (cycle [(if (= month 12) (+ 1 year) year)]))
                      (concat days-to-show-pc)
                      (into []))]
    all-days))

(defn- is-valid-date? [{:keys [day month year]}]
  (and (and day month year)
       (<= 0 year)
       (<= 1 month 12)
       (<= 1 day (no-of-days year month))))

(defn- in-range? 
  ([date {:keys [start end]} default]
    (let [{:keys [day month year]} date
      jsdate (js/Date. year (dec month) day)
      satisty-start? (if (and start (is-valid-date? start))
        (<= (js/Date. (:year start) (dec (:month start)) (:day start)) jsdate)
        default)
      satisty-end? (if (and end (is-valid-date? end))
        (<= jsdate (js/Date. (:year end) (dec (:month end)) (:day end)))
        default)]
      (and satisty-start? satisty-end?)))
  ([date range] (in-range? date range false)))

(defn- get-label 
  ([{:keys [day month year]}] 
    (str day
      " " (if month (subs ((month-names) (dec month)) 0 3))
      " " year)))

(defn- ensure-valid-date [date min max modifier-fn]
  (if (and date (is-valid-date? date) 
    (in-range? date {:start min :end max} true)) 
      (modifier-fn date)))

;; Helper Components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- day-fn [{:keys 
  [selected-range on-date-click on-date-mouse-enter on-date-mouse-leave]} 
    in-valid-range? day]
  ^{:key day}
  [:div (merge (stylefy/use-style (s-date in-valid-range?))
               (if in-valid-range? {:on-click #(on-date-click day)
                :on-mouse-enter #(on-date-mouse-enter day)
                :on-mouse-leave #(on-date-mouse-leave day)}))
   [:div (day :day)]
   [:div (stylefy/use-style
          (s-date-dots
           (cond
             (in-range? day (select-keys selected-range [:start :end]))
                (ht-style/colors :sky-blue)
             (in-range? day (set/rename-keys 
              (select-keys selected-range [:start :tempend]) {:tempend :end})) 
                (ht-style/colors :sky-blue)
             (or (= (:start selected-range) day)
                 (= (:start selected-range) day)) (ht-style/colors :sky-blue)
             :else "#F1F1F1")))]])

(defn- dates-selector [{:keys [year month week-start selected-range
                               on-date-click on-date-mouse-enter
                               on-date-mouse-leave valid-range]
                        :as props}]
  [:div [:div [:div
    (doall (->> (week-days)
      (cycle)
      (take (+ 7 week-start))
      (drop week-start)
      (map (fn [%]
        ^{:key %} [:div (stylefy/use-style s-week-header) (first %)]))))]]
    [:div
      (let [weeks (partition 7 (dates-to-show year month week-start))]
        (doall (map
          (fn [week]
            ^{:key week}
            [:div (doall (map 
              #(day-fn props (if valid-range (in-range? % valid-range true)) %) week))])
          weeks)))]])

(defn- months-component [on-month-change]
  [:div
   (map (fn [m]
          ^{:key m}
          [:div.month
           {:style {:display "inline-block"
                    :width "70px"
                    :text-align "center"
                    :padding "15px"
                    :color (ht-style/colors :bitumen-grey)}
            :on-click #(on-month-change m)}
           (subs m 0 3)])
        (month-names))])

(defn- full-toggle-button [text is-selected? onclick enabled?]
  [:div (merge (stylefy/use-style (s-full-toggle-button is-selected? enabled?))
               (if enabled? {:on-click #(onclick %)})) text])

;; Components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; calendar component
(defn calendar-component [{:keys 
  [selected-range on-change picker-type valid-range]
  {min-date :start max-date :end} :valid-range}]
  (let [
    state (r/atom {})

    ;; Functions ;;
    change-month (fn [func]
      (let [month (get-in @state [:month-to-show :month])]
        (cond
          (and (= func inc) (= month 12))
          (do
            (swap! state update-in [:month-to-show :year] inc)
            (swap! state assoc-in [:month-to-show :month] 1))
          (and (= func dec) (= month 1))
          (do
            (swap! state update-in [:month-to-show :year] dec)
            (swap! state assoc-in [:month-to-show :month] 12))
          :else (swap! state update-in [:month-to-show :month] func))))

    change-year (fn [func]
                  (swap! state update-in [:month-to-show :year] func))

    raise-on-change (fn []
        (on-change {:start (get-in @state [:selected-range :start])
                    :end (get-in @state [:selected-range :end])
                    :type (:range-option @state)}))

    on-date-click (fn [day]
      (let [picker-type (:picker-type @state)
            to-add (get-in range-options [(:range-option @state) :to-add])
            new-end (if (and (= picker-type :range) to-add) 
              (add-days day to-add dec))
            is-end-valid? (in-range? new-end {:start min-date :end max-date} true)
            valid-end (if is-end-valid? new-end max-date)
            valid-start (if is-end-valid? day (add-days valid-end 
              (update-values to-add * -1) inc))]
        (if (= picker-type :date) 
          (do (swap! state update :selected-range assoc :start day :end nil)
              (raise-on-change))
          (do (swap! state update :selected-range (fn [{:keys [start end]}]
                (cond (and start (not end)) 
                        {:start start :end day}
                      (and start end) 
                        {:start (if to-add valid-start day) :end (if to-add valid-end)}
                      (and (not start) (not end)) 
                        {:start (if to-add valid-start day) :end (if to-add valid-end)})))
              (if (and (get-in @state [:selected-range :start])
                       (get-in @state [:selected-range :end]))
                  (raise-on-change))))))

    on-date-mouse-enter (fn [day]
      (if (and (= (:range-option @state) :custom)
                (= picker-type :range)) 
        (if (and (get-in @state [:selected-range :start])
                  (not (get-in @state [:selected-range :end])))
          (swap! state assoc-in [:selected-range :tempend] day))))

    on-date-mouse-leave (fn [day]
      (if (and (= (:range-option @state) :custom)
                (= picker-type :range))
        (if (get-in @state [:selected-range :start])
          (swap! state assoc-in [:selected-range :tempend] nil))))

    on-month-change (fn [month]
      (let [month-no (->> (month-names)
                          (keep-indexed (fn [i m]
                                          (if (= m month) i)))
                          (first))]
        (swap! state assoc-in [:month-to-show :month] (+ 1 month-no)))
      (swap! state assoc-in [:view] :date))

    on-range-option-click (fn [option]
      (if (not= (:range-option @state) option)
        (do (swap! state assoc :range-option option)
            (swap! state assoc-in [:selected-range] :start nil :end nil))))

    on-year-change (fn [year]
      (swap! state assoc-in [:month-to-show :year] year))]

    (r/create-class
      { :component-did-mount
        (fn [_] 
          (swap! state assoc
            :picker-type picker-type
            :selected-range selected-range
            :range-option (or (:type selected-range) :custom)
            :view :date    ; or ":date/:month"
            :month-to-show (select-keys (or (:start selected-range) 
                             {:month (inc (.getMonth (js/Date.)))
                              :year (.getFullYear (js/Date.))}) [:month :year]) 
            :hide-content? false))

        :reagent-render
        (fn [ {:keys [selected-range on-change picker-type valid-range]
              {min-date :start max-date :end} :valid-range}]

          [:div {:style { :width "235px" :font-size "11px"
                          :user-select "none" :display "inline-block"}}
            [:div {:style {:padding "10px"}}
              [:div {:style {:text-align "center" :cursor "pointer"
                             :margin-bottom "15px"}}
                [:div {:style {:display "inline-block" :width "15px"}
                      :on-click (fn []
                        (swap! state assoc-in [:hide-content?] true)
                        (js/setTimeout #(if (= :date (get-in @state [:view]))
                                          (change-month dec)
                                          (change-year dec)) 100)
                        (js/setTimeout #(swap! state assoc-in [:hide-content?]
                                                false) 100))} "<"]
                [:div {:on-click #(swap! state assoc-in [:view] :month)
                      :style {:display "inline-block"
                              :text-align "center"
                              :width "100px"
                              :padding "2px"
                              :font-weight "bold"}}
                (if (= :date (get-in @state [:view]))
                  [:span 
                    (->> (get-in @state [:month-to-show :month]) 
                      (dec) ((month-names))) " " 
                      (get-in @state [:month-to-show :year])]
                  (get-in @state [:month-to-show :year]))]
                [:div {:style {:display "inline-block"
                              :width "15px" }
                      :on-click (fn []
                        (swap! state assoc-in [:hide-content?] true)
                        (js/setTimeout #(if (= :date (get-in @state [:view])) 
                                          (change-month inc)
                                          (change-year inc)) 100)
                        (js/setTimeout #(swap! state assoc-in [:hide-content?]
                                                false) 100))} ">"]]
            [:div {:class (str "s-opacity-transition"
              (if (:hide-content? @state) " s-transparent" nil))}
              (if (= :date (:view @state))
                [:div [dates-selector 
                    { :year (get-in @state [:month-to-show :year])
                      :valid-range valid-range
                      :month (get-in @state [:month-to-show :month])
                      :week-start 1
                      :selected-range (:selected-range @state)
                      :on-date-click on-date-click
                      :on-date-mouse-enter on-date-mouse-enter
                      :on-date-mouse-leave on-date-mouse-leave}]]
                [months-component on-month-change])]]
            (if (and (= :date (:view @state))
                    (= picker-type :range))
              [:div (stylefy/use-style s-range-options)
                (doall (map (fn [op] 
                  (let [option (op range-options)
                        to-add (:to-add option)]
                    ^{:key op}
                    [full-toggle-button (:label (op range-options))
                      (= (:range-option @state) op)
                      #(on-range-option-click op) 
                      (or (not to-add) (not min-date) (not max-date)
                        (in-range? (add-days min-date to-add dec) 
                          {:end max-date} true))])) 
                    (keys range-options)))])])})))


;; date-picker component
(defn date-picker [{:keys [date on-change min max valid? disabled?]
                    :or {valid? true disabled? false}}]
  (let [mystate (r/atom {})]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! mystate assoc :anchor (dom/dom-node this), :open? false))

      :reagent-render
      (fn [{:keys [date on-change min max valid?]
                    :or {valid? true disabled? false}}]
        [:div {:style {:display "inline-block" :vertical-align "top"}}
         [app-comp/action-input-box 
            { :disabled? disabled?, :valid? valid?, :width 100
              :label (ensure-valid-date date min max get-label)
              :action #(swap! mystate assoc :open? true)
              :right-icon ic/dropdown
              :right-action #(swap! mystate assoc :open? true)}]
         (if (:open? @mystate)
           [app-comp/popover 
             {:open (:open? @mystate)
              :on-request-close 
                #(swap! mystate assoc :open? false)
              :anchor-el (:anchor @mystate)
              :anchor-origin {:horizontal "right", 
                              :vertical "bottom"}
              :target-origin {:horizontal "right", 
                              :vertical "top"}}
              [calendar-component 
                { :selected-range {:start date :end nil}
                  :valid-range {:start min :end max}
                  :on-change (fn [{:keys [start]}]
                      (swap! mystate assoc :open? false)
                      (on-change (ensure-valid-date start min max identity)))
                  :picker-type :date}]])])})))


;; date-range-picker component
(defn date-range-picker [_]
  (let [mystate (r/atom {})
        to-add (fn [type] 
          (or (get-in range-options [type :to-add])
               {:days 1 :months 0 :year 0}))
        to-subtract (fn [type]
          (update-values (to-add type) * -1))]
    (r/create-class
     {:component-did-mount
      (fn [this] (swap! mystate assoc :anchor (dom/dom-node this), :open? false))

      :reagent-render
      (fn [{:keys 
        [selected-range on-change min max valid? disabled?]
            :or {valid? true disabled? false}
            {:keys [start end type]} :selected-range}]
        ;; check buttons
        (swap! mystate assoc 
          :right-disabled? (and max end 
              (in-range? (add-days end (to-add type) dec) {:start max} true))
          :left-disabled? (and min start 
              (in-range? (add-days start (to-subtract type) inc) {:end min} true)))
        [:div {:style {:display "inline-block" :vertical-align "top"}}
         [app-comp/action-input-box
           {:disabled? disabled?, :valid? valid?, :width "200px"
            :label (str (ensure-valid-date start min max get-label)
                " - " (ensure-valid-date end min max get-label))
            :action #(swap! mystate assoc :open? true)
            :left-icon ic/nav-left
            :left-disabled? (:left-disabled? @mystate)
            :left-action (fn [_]
              (let [new-start (add-days start (to-subtract type))
                    new-end (add-days (if (= type :custom) start new-start) 
                                    (to-add type) (if (= type :custom) identity dec))]
                    (on-change (assoc selected-range
                      :start new-start, :end new-end))))
            :right-icon ic/nav-right
            :right-disabled? (:right-disabled? @mystate)
            :right-action (fn [_]
              (let [new-start (add-days start (to-add type))
                    new-end (add-days (if (= type :custom) end new-start) 
                        (to-add type) (if (= type :custom) identity dec))]
                    (on-change (assoc selected-range
                      :start new-start, :end new-end))))}]
         (if (:open? @mystate)
           [app-comp/popover 
              { :open (:open? @mystate)
                :on-request-close #(swap! mystate assoc :open? false)
                :anchor-el (:anchor @mystate)
                :anchor-origin {:horizontal "right", :vertical "bottom"}
                :target-origin {:horizontal "right", :vertical "top"}}
              [calendar-component 
                { :selected-range selected-range
                  :valid-range {:start min :end max}
                  :on-change (fn [selected-range]
                    (swap! mystate assoc :open? false)
                    (on-change selected-range))
                  :picker-type :range}]])])})))
