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


(def ^:private s-week-header {:display "inline-block"
                              :width "30px"
                              :padding "5px"
                              :font-size "9px"
                              :vertical-align "top"
                              :text-align "center"
                              :color (ht-style/colors :slate-grey)})

(stylefy/class "s-opacity-transition"
               {:transition "opacity 0.1s"})

(stylefy/class "s-transparent"
               {:opacity "0"})

(def s-date {:display "inline-block"
             :width "30px"
             :padding "10px 5px "
             :vertical-align "top"
             :text-align "center"
             :color (ht-style/colors :royal-blue)})

(defn- s-date-dots [background-color]
  {:margin "auto"
   :margin-top "10px"
   :width "6px"
   :height "6px"
   :border-radius "3px"
   :background-color background-color})

(defn- s-full-toggle-button [is-selected?]
  (merge {:background-color (if is-selected?
                              (ht-style/colors :sky-blue)
                              (ht-style/colors :white))
          :color (if is-selected?
                   (ht-style/colors :white)
                   (ht-style/colors :sky-blue))
          :text-align "center"
          :display "inline-block"
          :width "56px"
          :font-size "9px"
          :height "26px"
          :padding-top "6px"
          :border-radius "13px"
          :margin "3px"
          :cursor "pointer"
          ::stylefy/mode
          {:hover {:background-color (ht-style/colors :alumina-grey)
                   :color (ht-style/colors :white)}}}))

(def s-range-options {:border-top "1px solid #CFD2D3"
                      :padding "10px"
                      :background-color "#F1F1F1"})

;;;;;;;;;; Styles End ;;;;;;;;;;


(def range-options
  [{:name "Custom" :to-add nil}
   {:name "1 week" :to-add {:days 7 :months 0 :years 0}}
   {:name "1 month" :to-add {:days 0 :months 1 :years 0}}
   {:name "6 months" :to-add {:days 0 :months 6 :years 0}}
   {:name "1 year" :to-add {:days 0 :months 0 :years 1}}])

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


(defn- is-leap-year? [year]
  (or (and
        (= 0 (mod year 4))
        (not= 0 (mod year 100)))
      (= 0 (mod year 400))))

(defn- no-of-days
  [year month]
  ([31 (if (is-leap-year? year) 29 28) 31 30 31 30 31 31 30 31 30 31] (dec month)))

(defn- get-day-of-week [year month day]
  (.getDay (js/Date. year (dec month) day)))

(defn add-days [{:keys [day month year]} {:keys [days months years]}]
  (let [jsDate (js/Date. year (dec month) day)]
    (.setYear jsDate (+ years (.getFullYear jsDate)))
    (.setMonth jsDate (+ months (.getMonth jsDate)))
    (.setDate jsDate (dec (+ days (.getDate jsDate))))
    {:day (.getDate jsDate)
     :month (inc (.getMonth jsDate))
     :year (.getFullYear jsDate)}))

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
                                    (range (->> days-pmonth-actual
                                                (- pmonth)
                                                (+ 1))
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

(defn- in-range? [date {:keys [start end]}]
  (let [{:keys [day month year]} date
        start-date (js/Date. (:year start) (dec (:month start)) (:day start))
        end-date (js/Date. (:year end) (dec (:month end)) (:day end))
        jsdate (js/Date. year (dec month) day)]
    (and start end (or (and (<= start-date jsdate) (<= jsdate end-date))
                       (and (<= end-date jsdate) (<= jsdate start-date))))))

(defn day-fn [{:keys [range on-date-click on-date-mouse-enter on-date-mouse-leave]}
              day]
  ^{:key day}
  [:div (merge (stylefy/use-style s-date)
               {:on-click #(on-date-click day)}
               {:on-mouse-enter #(on-date-mouse-enter day)}
               {:on-mouse-leave #(on-date-mouse-leave day)})
   [:div (day :day)]
   [:div (stylefy/use-style
          (s-date-dots
           (cond
             (in-range? day (select-keys range [:start :end])) (ht-style/colors :sky-blue)
             (in-range? day (set/rename-keys (select-keys range [:start :tempend]) {:tempend :end})) (ht-style/colors :sky-blue)
             (or (= (range :start) day)
                 (= (range :start) day)) "blue"
             :else "#F1F1F1")))]])

(defn- dates-selector [{:keys [year month week-start range
                               on-date-click on-date-mouse-enter
                               on-date-mouse-leave]
                        :as props}]
  [:div
   [:div
    [:div
     (doall
      (->> (week-days)
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
                [:div (doall (map #(day-fn props %) week))])
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

(defn- full-toggle-button [text is-selected? onclick]
  [:div (merge (stylefy/use-style (s-full-toggle-button is-selected?))
               {:on-click #(onclick %)})
   text])

(defn calendar-component [{:keys [selection-complete-event selection]}]
  (let [state (r/atom {:view "date"     ; or "date/month"
                       :month-to-show {:month (inc (.getMonth (js/Date.)))
                                       :year (.getFullYear (js/Date.))}
                       :range {:start nil
                               :end nil
                               :tempend nil}
                       :selected-range "1 month"
                       :hide-content? false})

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

        raise-selection-complete-event (fn []
                                         (let [start (get-in @state [:range :start])
                                               end (get-in @state [:range :end])]
                                           (selection-complete-event {:start start
                                                                      :end end})))

        on-date-click (fn [day]
                        (let [selected-range (:selected-range @state)
                              to-add (some #(when (= (:name %) selected-range) (:to-add %))
                                           range-options)]
                          (if (= selection "range")
                            (if (not (get-in @state [:range :start]))
                              (do (swap! state assoc-in [:range :start] day)
                                  (if to-add (do
                                               (swap! state assoc-in [:range :end]
                                                      (add-days day to-add))
                                               (raise-selection-complete-event))))
                              (if (not (get-in @state [:range :end]))
                                (do
                                  (swap! state assoc-in [:range :end] day)
                                  (raise-selection-complete-event))
                                (do (swap! state assoc-in [:range :start] day)
                                    (swap! state assoc-in [:range :end] nil)
                                    (if to-add
                                      (do
                                        (swap! state assoc-in [:range :end]
                                               (add-days day to-add))
                                        (raise-selection-complete-event))))))
                            (do
                              (swap! state assoc-in [:range :start] day)
                              (raise-selection-complete-event)))))

        on-date-mouse-enter (fn [day]
                              (if (and (= (:selected-range @state) "Custom")
                                       (= selection "range")) 
                                (if (and (get-in @state [:range :start])
                                         (not (get-in @state [:range :end])))
                                  (swap! state assoc-in [:range :tempend] day))))

        on-date-mouse-leave (fn [day]
                              (if (and (= (:selected-range @state) "Custom")
                                       (= selection "range"))
                                (if (get-in @state [:range :start])
                                  (swap! state assoc-in [:range :tempend] nil))))

        on-month-change (fn [month]
                          (let [month-no (->> (month-names)
                                              (keep-indexed (fn [i m]
                                                              (if (= m month) i)))
                                              (first))]
                            (swap! state assoc-in [:month-to-show :month] (+ 1 month-no)))
                          (swap! state assoc-in [:view] "date"))

        on-range-option-click (fn [button]
                                (swap! state assoc-in [:selected-range] (:name button))
                                (if (= (:name button) "Custom")
                                  (swap! state assoc-in [:range] {:start nil :end nil}))
                                (let [to-add (some #(when (= (:name %) (:name button))
                                                      (:to-add %))
                                                   range-options)
                                      day (get-in @state [:range :start])]
                                  (if (and day to-add (= selection "range"))
                                    (do
                                      (swap! state assoc-in [:range :end]
                                             (add-days day to-add))
                                      (raise-selection-complete-event)))))

        on-year-change (fn [year]
                         (swap! state assoc-in [:month-to-show :year] year))]

    (r/create-class
     {:reagent-render
      (fn []
        (let [selected-month (get-in @state [:month-to-show :month])
              selected-year (get-in @state [:month-to-show :year])
              selected-range (@state :selected-range)]
          [:div {:style {:width "235px"
                         :font-size "11px"
                         :user-select "none"
                         :display "inline-block"}}
           [:div {:style {:padding "10px"}}
            [:div {:style {:text-align "center"
                           :cursor "pointer"
                           :margin-bottom "15px"}}
             [:div {:style {:display "inline-block"
                            :width "15px"}
                    :on-click (fn []
                                (swap! state assoc-in [:hide-content?] true)
                                (js/setTimeout #(if (= "date" (get-in @state [:view]))
                                                  (change-month dec)
                                                  (change-year dec))
                                               100)
                                (js/setTimeout #(swap! state assoc-in [:hide-content?]
                                                       false)
                                               100))}
              "<"]
             [:div {:on-click #(swap! state assoc-in [:view] "month")
                    :style {:display "inline-block"
                            :text-align "center"
                            :width "100px"
                            :padding "2px"
                            :font-weight "bold"}}
              (if (= "date" (get-in @state [:view]))
                [:span (->> selected-month (dec) ((month-names))) " " selected-year]
                (get-in @state [:month-to-show :year]))]
             [:div {:style {:display "inline-block"
                            :width "15px" }
                    :on-click (fn []
                                (swap! state assoc-in [:hide-content?] true)
                                (js/setTimeout #(if (= "date" (get-in @state [:view])) 
                                                  (change-month inc)
                                                  (change-year inc))
                                               100)
                                (js/setTimeout #(swap! state assoc-in [:hide-content?]
                                                       false)
                                               100))}
              ">"]]
            [:div {:class (str "s-opacity-transition"
                               (if (:hide-content? @state) " s-transparent" nil))}
             (if (= "date" (:view @state))
               [:div
                [dates-selector {:year selected-year
                                 :month selected-month
                                 :week-start 1
                                 :range (:range @state)
                                 :on-date-click on-date-click
                                 :on-date-mouse-enter on-date-mouse-enter
                                 :on-date-mouse-leave on-date-mouse-leave}]]
               [months-component on-month-change])]]
           (if (and (= "date" (:view @state))
                    (= selection "range"))
             [:div (stylefy/use-style s-range-options)
              (map (fn [op]
                     ^{:key (:name op)}
                     [full-toggle-button
                      (:name op)
                      (= selected-range (:name op))
                      #(on-range-option-click op)])
                   range-options)])]))})))

;; Components ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn date-picker [{:keys [date]}]
  (let [mystate (r/atom {})
        get-label (fn [{:keys [day month year]}]
                    (str day
                         " " (if month
                               (subs ((month-names) (dec month)) 0 3))
                         " " year))]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! mystate assoc :anchor (dom/dom-node this))
        (swap! mystate assoc :open? false)
        (swap! mystate assoc :date date))

      :reagent-render
      (fn [{:keys [on-select]}]
        [:div {:style {:display "inline-block" :vertical-align "top"}}
         [app-comp/action-input-box {:disabled? false
                                     :width 100
                                     :label (get-label (:date @mystate))
                                     :action #(swap! mystate assoc :open? true)
                                     :right-icon ic/dropdown
                                     :right-action #(swap! mystate assoc :open? true)}]
         (if (:open? @mystate)
           [app-comp/popover {:open (:open? @mystate)
                              :on-request-close #(swap! mystate assoc :open? false)
                              :anchor-el (:anchor @mystate)
                              :anchor-origin {:horizontal "right", :vertical "bottom"}
                              :target-origin {:horizontal "right", :vertical "top"}}
            [calendar-component {:selection-complete-event
                                 (fn [{:keys [start]}]
                                   ;; (js/console.log start)
                                   (swap! mystate assoc :date start)
                                   (swap! mystate assoc :open? false)
                                   (on-select start))
                                 :selection "date"}]])])})))

(defn date-range-picker [{:keys [start end]}]
  (let [state (r/atom {})
        get-label (fn [{:keys [start end]}]
                    (str (:day start)
                         " " (if (:month start)
                               (subs ((month-names) (dec (:month start))) 0 3))
                         " " (:year start) " - " (:day end)
                         " " (if (:month end)
                               (subs ((month-names) (dec (:month end))) 0 3))
                         " " (:year end)))]
    (r/create-class
     {:component-did-mount
      (fn [this]
        (swap! state assoc :anchor (dom/dom-node this))
        (swap! state assoc :open? false)
        (swap! state assoc :range {:start start :end end}))

      :reagent-render
      (fn [{:keys [on-select]}]
        [:div {:style {:display "inline-block"
                       :vertical-align "top"}}
         [app-comp/action-input-box
          {:disabled? false
           :width "200px"
           :label (get-label (:range @state))
           :action #(swap! state assoc :open? true)
           :left-icon ic/nav-left
           :left-action (fn [_]
                          (swap! state update-in [:range :start]
                                 add-days {:days 0 :months 0 :years 0})
                          (swap! state update-in [:range :end]
                                 add-days {:days 0 :months 0 :years 0})
                          (on-select (:range @state)))
           :right-icon ic/nav-right
           :right-action (fn [_]
                           (swap! state update-in [:range :start]
                                  add-days {:days 2 :months 0 :years 0})
                           (swap! state update-in [:range :end]
                                  add-days {:days 2 :months 0 :years 0})
                           (on-select (:range @state)))}]
         (if (:open? @state)
           [app-comp/popover {:open true
                              :on-request-close #(swap! state assoc :open? false)
                              :anchor-el (:anchor @state)
                              :anchor-origin {:horizontal "right", :vertical "bottom"}
                              :target-origin {:horizontal "right", :vertical "top"}}
            [calendar-component {:selection-complete-event
                                 (fn [r]
                                   ;; (js/console.log r)
                                   (swap! state assoc :range r)
                                   (swap! state assoc :open? false)
                                   (on-select r))
                                 :selection "range"}]])])})))
