;; subscriptions for component config
(ns tta.component.config.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :as rr]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]
            [tta.util.common :as au]
            [clojure.string :as str]))

(rf/reg-sub
  ::src-data
  :<- [::app-subs/plant]
  (fn [plant _] (or (:config plant) {:draft? true})))

(rf/reg-sub
 ::component
 (fn [db _] (get-in db [:component :config])))

(rf/reg-sub
 ::show-error? ;; used for hiding errors until first click on submit
 :<- [::component]
 (fn [component _] (:show-error? component)))

(rf/reg-sub
 ::data
 :<- [::src-data]
 :<- [::component]
 (fn [[src-data component] _] (or (:data component) src-data)))

(rf/reg-sub
 ::form
 :<- [::component]
 (fn [component _] (:form component)))

(rf/reg-sub
 ::draft?
 :<- [::data]
 (fn [data _] (:draft? data)))

(rf/reg-sub
  ::dirty?
  :<- [::data]
  :<- [::src-data]
  (fn [[data src-data] _] (not= data src-data)))

(rf/reg-sub
  ::valid?
  :<- [::form]
  (fn [form _] (not (au/some-invalid form))))

(rf/reg-sub
  ::can-submit?
  :<- [::dirty?]
  :<- [::valid?]
  :<- [::draft?]
  :<- [::view-factor-valid?]
  (fn [[dirty? valid? draft? vf-valid?] _]
    ;; additional check, top-fired & not draft? => view-factor full!
    (and dirty? valid?
         (or draft? vf-valid?))))

(rf/reg-sub
  ::warn-on-close?
  :<- [::dirty?]
  :<- [::valid?]
  (fn [[dirty? valid?] _] (or dirty? (not valid?))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-field
  ([path form data] (get-field path form data identity))
  ([path form data parse]
   (or (get-in form path)
       {:value (parse (get-in data path))
        :valid? true})))

(rf/reg-sub
  ::field
  :<- [::form]
  :<- [::data]
  (fn [[form data] [_ path]] (get-field path form data)))

(defn reformer-name-field []
  (rf/subscribe [::field [:name]]))

(rf/reg-sub
  ::firing
  :<- [::data]
  (fn [data _] (:firing data)))

(defn firing-field []
  (rf/subscribe [::field [:firing]]))

(rf/reg-sub
 ::firing-options
 :<- [::ht-subs/translation [:config :firing :side]]
 :<- [::ht-subs/translation [:config :firing :top]]
  (fn [[side top] _]
    [{:id "side" :name (or side "Side fired")}
     {:id "top" :name (or top "Top fired")}]))

(rf/reg-sub
 ::sketch-config
 :<- [::data]
 (fn [data _]
   (case (:firing data)
     "side" (dissoc data :tf-config)
     "top" (dissoc data :sf-config)
     {})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-numbers-options
  "returns the list of possible numbers options.
  each option is a map {:id id-string, :label label-string, :nums [start end]}"
  [row-count row-length]
  (if (and row-count row-length)
    (->> (range row-count)
         (mapcat #(let [n (* row-length %)
                        start (inc n)
                        end (+ n row-length)]
                    (list {:id (str % 0)
                           :nums [start end]
                           :label (str start "→" end)}
                          {:id (str % 1)
                           :nums [end start]
                           :label (str end "←" start)}))))
    []))

(defn get-numbers-selection
  "returns the form field with id corresponding to the selected option"
  [row-path form data options row-type]
  (let [[start-key end-key sel-key]
        (case row-type
          :tube [:start-tube :end-tube :tube-numbers-selection]
          :burner [:start-burner :end-burner :burner-numbers-selection])]
    (or (get-in form (conj row-path sel-key))
        (let [{start start-key, end end-key} (get-in data row-path)]
          (some (fn [{:keys [id nums]}]
                  (if (= nums [start end]) id))
                options)))))

;;; TOP-FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::tf-measure-level?
 :<- [::data]
 (fn [data [_ level-key]] ;; level-key: :top?, :middle?, :bottom?
   (get-in data [:tf-config :measure-levels level-key])))

(rf/reg-sub
 ::tf-measure-levels-validity
 :<- [::form]
 (fn [form _]
   (get-in form [:tf-config :measure-levels-validity])))

(rf/reg-sub
 ::tf-burner-first?
 :<- [::data]
 (fn [data _]
   (get-in data [:tf-config :burner-first?])))

(rf/reg-sub
 ::tf-wall-name-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] [_ wall-key]]
   (get-field [:tf-config :wall-names wall-key] form data)))

;; tubes
(rf/reg-sub
 ::tf-tube-row-count
 :<- [::data]
 (fn [data _]
   (get-in data [:tf-config :tube-row-count])))

(rf/reg-sub
 ::tf-tube-row-count-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (get-field [:tf-config :tube-row-count] form data)))

(rf/reg-sub
 ::tf-tube-count-per-row
 :<- [::data]
 (fn [data _]
   (get-in data [:tf-config :tube-rows 0 :tube-count])))

(rf/reg-sub
 ::tf-tube-count-per-row-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (get-field [:tf-config :tube-rows 0 :tube-count] form data)))

(rf/reg-sub
 ::tf-tube-numbers-options
 :<- [::tf-tube-row-count]
 :<- [::tf-tube-count-per-row]
 (fn [[row-count tube-count] _]
   (get-numbers-options row-count tube-count)))

(rf/reg-sub
 ::tf-tube-numbers-selection
 :<- [::data]
 :<- [::form]
 :<- [::tf-tube-numbers-options]
 (fn [[data form opts] [_ row-index]]
   (get-numbers-selection [:tf-config :tube-rows row-index]
                          form data opts :tube)))

;; burners
(rf/reg-sub
 ::tf-burner-row-count
 :<- [::data]
 (fn [data _]
   (get-in data [:tf-config :burner-row-count])))

(rf/reg-sub
 ::tf-burner-row-count-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (get-field [:tf-config :burner-row-count] form data)))

(rf/reg-sub
 ::tf-burner-count-per-row
 :<- [::data]
 (fn [data _]
   (get-in data [:tf-config :burner-rows 0 :burner-count])))

(rf/reg-sub
 ::tf-burner-count-per-row-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (get-field [:tf-config :burner-rows 0 :burner-count] form data)))

(rf/reg-sub
 ::tf-burner-numbers-options
 :<- [::tf-burner-row-count]
 :<- [::tf-burner-count-per-row]
 (fn [[row-count burner-count] _]
   (get-numbers-options row-count burner-count)))

(rf/reg-sub
 ::tf-burner-numbers-selection
 :<- [::data]
 :<- [::form]
 :<- [::tf-burner-numbers-options]
 (fn [[data form opts] [_ row-index]]
   (get-numbers-selection [:tf-config :burner-rows row-index]
                          form data opts :burner)))

;;; SIDE-FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::sf-placement-of-WHS
 :<- [::data]
 (fn [data _]
   (get-in data [:sf-config :placement-of-WHS])))

(rf/reg-sub
 ::sf-dual-nozzle?
 :<- [::data]
 (fn [data _]
   (get-in data [:sf-config :dual-nozzle?])))

(rf/reg-sub
 ::sf-dual-chamber?
 :<- [::data]
 (fn [data _]
   (= 2 (count (get-in data [:sf-config :chambers])))))

(defn sf-chamber-name-field [ch-index]
  (rf/subscribe [::field [:sf-config :chambers ch-index :name]]))

(defn sf-side-name-field [ch-index s-index]
  (rf/subscribe [::field [:sf-config :chambers ch-index :side-names s-index]]))

(rf/reg-sub
 ::sf-section-count-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (get-field [:sf-config :chambers 0 :section-count] form data)))

(rf/reg-sub
 ::sf-pd-tube-count-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] [_ pd-index]]
   (get-field [:sf-config :chambers 0 :peep-door-tube-count pd-index]
              form data)))

(rf/reg-sub
 ::sf-pd-count-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (get-field [:sf-config :chambers 0 :peep-door-count] form data)))

;; tubes
(rf/reg-sub
 ::sf-tube-count
 :<- [::data]
 (fn [data _]
   (get-in data [:sf-config :chambers 0 :tube-count])))

(rf/reg-sub
 ::sf-tube-count-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (get-field [:sf-config :chambers 0 :tube-count] form data)))

(rf/reg-sub
 ::sf-tube-numbers-options
 :<- [::sf-dual-chamber?]
 :<- [::sf-tube-count]
 (fn [[dual? tcount]]
   (get-numbers-options (if dual? 2 1) tcount)))

(rf/reg-sub
 ::sf-tube-numbers-selection
 :<- [::data]
 :<- [::form]
 :<- [::sf-tube-numbers-options]
 (fn [[data form opts] [_ ch-index]]
   (get-numbers-selection [:sf-config :chambers ch-index]
                          form data opts :tube)))

;; burners
(rf/reg-sub
 ::sf-burner-count-per-row
 :<- [::data]
 (fn [data _]
   (get-in data [:sf-config :chambers 0 :burner-count-per-row])))

(rf/reg-sub
 ::sf-burner-count-per-row-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (get-field [:sf-config :chambers 0 :burner-count-per-row] form data)))

(rf/reg-sub
 ::sf-burner-numbers-options
 :<- [::sf-dual-chamber?]
 :<- [::sf-burner-count-per-row]
 (fn [[dual? bcount]]
   (get-numbers-options (if dual? 2 1) bcount)))

(rf/reg-sub
 ::sf-burner-numbers-selection
 :<- [::data]
 :<- [::form]
 :<- [::sf-burner-numbers-options]
 (fn [[data form opts] [_ ch-index]]
   (get-numbers-selection [:sf-config :chambers ch-index]
                          form data opts :burner)))

(rf/reg-sub
 ::sf-burner-row-count-field
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (get-field [:sf-config :chambers 0 :burner-row-count] form data)))

;; pseudo field for validity
(rf/reg-sub
 ::sf-chamber-validity
 :<- [::form]
 (fn [form _]
   (get-in form [:sf-config :chamber-validity])))

(rf/reg-sub
 ::sf-pd-count-per-section
 :<- [::data]
 :<- [::form]
 (fn [[data form] _]
   (let [fch (get-in form [:sf-config :chambers 0])
         ch (get-in data [:sf-config :chambers 0])
         chk? (some-fn nil? :valid?)
         sn (if (chk? (:section-count fch)) (:section-count ch))
         pdn (if (chk? (:peep-door-count fch)) (:peep-door-count ch))
         pdn (if (and sn pdn (zero? (mod pdn sn))) (quot pdn sn))]
     pdn)))

;; VIEW-FACTOR (TF ONLY) ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; darft can be saved without view-factor
;; completion of view-factor auto-commits draft
;; once committed view-factor 100% mandatory

;; valid config to work on view-factor
(rf/reg-sub
 ::ready-for-view-factor?
 :<- [::tf-tube-count-per-row]
 :<- [::tf-tube-row-count]
 :<- [::tf-measure-levels-validity]
 (fn [[tube-count row-count validity] _]
   (and (pos? tube-count)
        (pos? row-count)
        (or (nil? validity) (:valid? validity)))))
(rf/reg-sub
 ::view-factor-valid?
 :<- [::firing]
 :<- [::tf-view-factor-full?]
 :<- [::tf-view-factor-matching-config?]
 (fn [[firing vf-full? vf-match?] _]
   (or (= firing "side")
       (and vf-full? vf-match?))))

(rf/reg-sub
 ::view-factor
 :<- [::data]
 (fn [data _]
   (get-in data [:tf-config :view-factor])))

(rf/reg-sub
 ::tf-view-factor-full?
 :<- [::view-factor]
 :<- [::tf-measure-level? :top?]
 :<- [::tf-measure-level? :middle?]
 :<- [::tf-measure-level? :bottom?]
 (fn [[vf top? middle? bottom?] _]
   (let [;; function to check a level, returns true if any missing
         f (fn [level-key]
             (if-let [rows (get-in vf [level-key :tube-rows])]
               (some (fn [row]
                       ;; each row is a map of :wall/:ceiling/:floor
                       ;; to a 2D array side x tube.
                       ;; no need to check for existence of keys, since it is
                       ;; assummed that the row will always initialized
                       ;; with a full empty skeleton by the view-factor dialog.
                       ;; hence enough to check the values only.
                       (some (fn [sides]
                               (some (fn [tubes]
                                       (some nil? tubes))
                                     sides))
                             (vals row)))
                     rows)
               true))]
     ;; check that no required level missing
     (not (or
           (if top? (f :top))
           (if middle? (f :middle))
           (if bottom? (f :bottom)))))))

(rf/reg-sub
 ::tf-view-factor-matching-config?
 :<- [::view-factor]
 :<- [::tf-tube-row-count]
 :<- [::tf-tube-count-per-row]
 :<- [::tf-measure-level? :top?]
 :<- [::tf-measure-level? :middle?]
 :<- [::tf-measure-level? :bottom?]
 (fn [[vf rn tn top? middle? bottom?] _]
   ;; if vf not available yet, then there is no mismatch
   ;; need to check for mismatch only when present
   (let [level-keys (remove nil? [(if top? :top)
                                  (if middle? :middle)
                                  (if bottom? :bottom)])]
     (or (not vf)
         (not ;; should have no mismatch
          (or
           ;; bad if list of levels not same
           (not= (into #{} level-keys)
                 (into #{} (keys vf)))
           ;; now check if any level doesn't match in terms off
           ;; tube-row-count or tube-count
           (some (fn [level-key]
                   (let [rows (get-in vf [level-key :tube-rows])]
                     (or
                      ;; bad if tube row count has changed
                      (not= (count rows) rn)
                      ;; just check length of one side of the wall
                      ;; its enough since rows are always created
                      ;; with same tube-count for wall as well as ceiling/floor
                      (some (fn [wall]
                              (not= (count (first wall)) tn))
                            (map :wall rows)))))
                 level-keys)))))))
