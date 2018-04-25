(ns tta.component.dataset.calc)

(def ^:const K 273.15)


;; process an array of wall temps: remove nils and calc avg
;; avg - root 1/4 of avg of power 4 of temp in K
(defn- calc-wall-temps [{:keys [temps]}]
  (let [temps (vec (remove nil? temps))
        n (count temps)
        avg (if (pos? n)
              (->> temps
                   (map #(let [k (+ K %)] (* k k k k))) ;; 4th power of kelvin
                   (reduce +)  ;; sum
                   ;; divide by n -> 4th root -> back to degC
                   (#(- (js/Math.pow (/ % n) 0.25) K))))]
    {:avg avg
     :temps temps}))

(defn- apply-emissivity [tube-row row-emissivity emissivity-type]
  (update tube-row :sides
          (fn [sides]
            (if (= emissivity-type "common")
              ;; clear emissivity from each tube, since common will be used
              (mapv (fn [side]
                      (update side :tubes
                              (fn [tubes]
                                (mapv #(dissoc % :emissivity) tubes))))
                    sides)
              ;; add custom or gold-cup emissivty to each tube
              (mapv (fn [side es]
                      (update side :tubes
                              (fn [tubes]
                                (mapv #(assoc %1 :emissivity %2)
                                      tubes (or es (repeat nil))))))
                    sides
                    (get row-emissivity (case emissivity-type
                                            "custom" :custom-emissivity
                                            :gold-cup-emissivity)
                         (repeat nil)))))))

(defn- calc-dT-corr
  "calculate the correction delta in degC (same as K)  
  Tm : raw-temp
  Tw : avg-wall-temp
  lam : wavelength (λ, μm)
  em : emissivity
  F : view-factor"
  [Tm Tw lam em F]
  (if (= Tm Tw) 0
      (if (and (pos? Tm) (pos? Tw) (pos? lam) (<= 0 em 1)
               (< Tm Tw) (<= 0 F 1))
        (let [C 14388 ;; constant
              Tw (+ K Tw)
              Tm (+ K Tm)
              X (/ C lam)
              Y (- (js/Math.exp (/ C lam Tm)) 1)
              Z (* F (- 1 em))
              Q (- (js/Math.exp (/ C lam Tw)) 1)
              a (- (/ 1 Y) (/ Z Q))
              Tc (/ X (js/Math.log (+ (/ em a) 1)))]
          (- Tm Tc)))))

(defn- avg [nums]
  (let [nums (remove nil? nums)
        n (count nums)]
    (if (pos? n)
      (/ (apply + nums) n))))

(defn- pct [x n]
  (if (and (pos? x) (pos? n))
    (* 100.0 (/ x n))))

(defn- tube-row-summary
  "return summary for a row of tubes.  
  in case of top-fired it is a tube-row.
  in case of side-fired it is a chamber."
  [tube-row]
  (let [tn (count (get-in tube-row [:sides 0 :tubes]))
        ts (mapcat :tubes (:sides tube-row))
        ;; collect raw-temp
        rts (filter pos? (map :raw-temp ts))
        rtn (count rts)
        rt+ (apply + rts)
        ;; collect corrected-temp
        cts (filter pos? (map :corrected-temp ts))
        ctn (count cts)
        ct+ (apply + cts)
        ;; gold-cup count
        gcn (count (filter (some-fn :emissivity-override
                                    :emissivity-calculated)
                           ts))
        ;; measured-tube count
        mtn (->> (map :tubes (:sides tube-row))
                 ;; make a tube list (each a pair of side A & B)
                 (apply map list)
                 ;; tube is considered to be measured if
                 ;; at least one side raw-temp is available
                 (filter (fn [[ta tb]]
                           (or (pos? (:raw-temp ta))
                               (pos? (:raw-temp tb)))))
                 count)]
    (cond-> {:t-count tn ;; tube-count
             :mt-count mtn ;; measured tube-count
             :gc-count gcn ;; gold-cup count
             ;; raw-temp count and sum (only pos?)
             :rt-count rtn, :rt-sum rt+
             ;; corrected-temp count and sum (only pos?)
             :ct-count ctn, :ct-sum ct+
             ;; summary
             :tubes% (or (pct mtn tn) 0)
             :gold-cup% (or (pct gcn (* 2 tn)) 0)}
      (pos? ctn) (assoc :avg-temp (/ ct+ ctn)
                        :max-temp (apply max cts)
                        :min-temp (apply min cts) )
      (pos? rtn) (assoc :avg-raw-temp (/ rt+ rtn)
                        :max-raw-temp (apply max rts)
                        :min-raw-temp (apply min rts)))))

(defn- summary [row-summary]
  (let [rs row-summary
        tn (apply + (map :t-count rs))
        mtn (apply + (map :mt-count rs))
        gcn (apply + (map :gc-count rs))
        rtn (apply + (map :rt-count rs))
        ctn (apply + (map :ct-count rs))
        rt+ (apply + (map :rt-sum rs))
        ct+ (apply + (map :ct-sum rs))
        sfn (fn [k f]
              (->> (map k rs)
                   (remove nil?)
                   (apply f)))]
    (cond-> {:tubes% (or (pct mtn tn) 0)
             :gold-cup% (or (pct gcn (* 2 tn)) 0)
             :rows (mapv #(dissoc % :t-count :mt-count :gc-count
                                  :rt-count :rt-sum :ct-count :ct-sum)
                                rs)}
      (pos? ctn) (assoc :avg-temp (/ ct+ ctn)
                        :max-temp (sfn :max-temp max)
                        :min-temp (sfn :min-temp min))
      (pos? rtn) (assoc :avg-raw-temp (/ rt+ rtn)
                        :max-raw-temp (sfn :max-raw-temp max)
                        :min-raw-temp (sfn :min-raw-temp min)))))

(defn update-summary [dataset]
  (assoc dataset :summary
         (if-let [chambers (get-in dataset [:side-fired :chambers])]
           ;; side-fired
           (summary (map tube-row-summary chambers))
           ;; top-fired
           (let [[rs kvs] (->> (get-in dataset [:top-fired :levels])
                               (map (fn [[level-key level]]
                                      (let [rs (map tube-row-summary (:rows level))]
                                        [rs [level-key (summary rs)]])))
                               (apply map list))]
             (-> (summary (apply concat rs))
                 (dissoc :rows)
                 (assoc :levels (into {} kvs)))))))

;; TOP-FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn tf-calc-wall-temps [dataset]
  (-> dataset
      ;; process 4 wall temps
      (update-in [:top-fired :wall-temps]
                 (fn [wall-temps]
                   (reduce #(update %1 %2 calc-wall-temps)
                           wall-temps
                           (keys wall-temps))))
      ;; process ceiling temps
      (update-in [:top-fired :ceiling-temps] #(if % (mapv calc-wall-temps %)))
      ;; process floor temps
      (update-in [:top-fired :floor-temps] #(if % (mapv calc-wall-temps %)))))

(defn- tf-levels-update-rows
  "f : (fn [tube-rows level-key])"
  [dataset f]
  (update-in dataset [:top-fired :levels]
             (fn [levels]
               (reduce (fn [levels level-key]
                         (update-in levels [level-key :rows] f level-key))
                       levels (keys levels)))))

(defn- tf-tube-row-apply-view-factor
  "add view-factor to each tube as found in view-factor data for the tube-row.
  the view-factor for each tube on each side is a pair of
  1- wall view-factor & 2- ceiling/floor view-factor"
  [tube-row row-view-factor]
  (let [{:keys [wall ceiling floor]} row-view-factor]
    (update tube-row :sides
            (fn [sides]
              (mapv (fn [side wall ceiling floor]
                      (update side :tubes
                              (fn [tubes]
                                (mapv #(assoc %1 :view-factor [%2 (or %3 %4)])
                                      tubes
                                      (or wall (repeat nil))
                                      (or ceiling (repeat nil))
                                      (or floor (repeat nil))))))
                    sides
                    (or wall (repeat nil))
                    (or ceiling (repeat nil))
                    (or floor (repeat nil)))))))

(defn tf-apply-view-factor [dataset config]
  (tf-levels-update-rows
   dataset
   (fn [rows level-key]
     (mapv #(tf-tube-row-apply-view-factor %1 %2)
           rows
           (get-in config [:tf-config :view-factor level-key :tube-rows])))))

(defn tf-apply-emissivity [dataset settings]
  (tf-levels-update-rows
   dataset
   (fn [rows level-key]
     (mapv #(apply-emissivity %1 %2 (:emissivity-type dataset))
           rows
           (get-in settings [:tf-settings :levels level-key :tube-rows])))))

(defn- tf-update-tubes
  "f : (fn [tubes ri si])"
  [rows f]
  (->> rows
       (map-indexed
        (fn [ri row]
          (update row :sides
                  (fn [sides]
                    (->> sides
                         (map-indexed
                          (fn [si side]
                            (update side :tubes f ri si)))
                         vec)))))
       vec))

(defn- tf-split-tubes [tubes]
  (let [n (count tubes)]
    [(take 6 tubes)
     (take (- n 12) (drop 6 tubes))
     (drop (- n 6) tubes)]))

(defn- tf-calc-tube [dataset tube Tw]
  (let [py (:pyrometer dataset)
        em (or (:emissivity-override tube)
               (:emissivity-calculated tube)
               (:emissivity tube)
               (:emissivity dataset)
               (:tube-emissivity py))
        lam (:wavelength py)
        Tm (:raw-temp tube)
        F (:view-factor tube)
        dT (->> (map #(calc-dT-corr Tm %1 lam em %2) Tw F)
                (remove nil?) not-empty)
        Tc (if dT (+ Tm (apply + dT)))]
    (assoc tube :corrected-temp Tc)))

(defn tf-calc-Tc [dataset]
  (let [Tw-n (get-in dataset [:top-fired :wall-temps :north :avg])
        Tw-e (get-in dataset [:top-fired :wall-temps :east :avg])
        Tw-s (get-in dataset [:top-fired :wall-temps :south :avg])
        Tw-w (get-in dataset [:top-fired :wall-temps :west :avg])
        Tw-nw (avg [Tw-n Tw-w])
        Tw-ne (avg [Tw-n Tw-e])
        Tw-sw (avg [Tw-s Tw-w])
        Tw-se (avg [Tw-s Tw-e])
        Tw-n6 [Tw-nw Tw-ne]
        Tw-m [Tw-w Tw-e]
        Tw-s6 [Tw-sw Tw-se]
        Tw-c (mapv :avg (get-in dataset [:top-fired :ceiling-temps]))
        Tw-f (mapv :avg (get-in dataset [:top-fired :floor-temps]))
        calc-tube (partial tf-calc-tube dataset)
        calc-tubes-fn (fn [level-key]
                        (let [Tw-2 (case level-key
                                     :top Tw-c
                                     :bottom Tw-f
                                     nil)]
                          (fn [tubes ri si]
                            (let [[nts mts sts] (tf-split-tubes tubes)
                                  Tw-n6 (get Tw-n6 si)
                                  Tw-m (get Tw-m si)
                                  Tw-s6 (get Tw-s6 si)
                                  Tw-2 (get Tw-2 (+ ri si))]
                              (vec (concat
                                    (map #(calc-tube % [Tw-n6 Tw-2]) nts)
                                    (map #(calc-tube % [Tw-m Tw-2]) mts)
                                    (map #(calc-tube % [Tw-s6 Tw-2]) sts)))))))]
    (reduce
     (fn [dataset level-key]
       (update-in dataset [:top-fired :levels level-key :rows]
                  tf-update-tubes
                  (calc-tubes-fn level-key)))
     dataset
     (keys (get-in dataset [:top-fired :levels])))))

;; SIDE-FIRED ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- sf-update-sides
  "f : (fn [side ci si])"
  [dataset f]
  (update-in dataset [:side-fired :chambers]
             (fn [chambers]
               (vec (map-indexed (fn [ci c]
                                   (update c :sides
                                           (fn [sides]
                                             (vec (map-indexed (fn [si s]
                                                                 (f s ci si))
                                                               sides)))))
                                 chambers)))))

(defn sf-calc-wall-temps [dataset]
  (sf-update-sides dataset
                   (fn [side _ _]
                     (update side :wall-temps
                             #(mapv calc-wall-temps %)))))

(defn sf-apply-emissivity [dataset settings]
  (update-in dataset [:side-fired :chambers]
             (fn [chambers]
               (mapv #(apply-emissivity %1 %2 (:emissivity-type dataset))
                     chambers
                     (get-in settings [:sf-settings :chambers])))))

(defn- sf-split-tubes [tubes peep-door-tube-count]
  (loop [[n & r] peep-door-tube-count
         tubes tubes
         ts []]
    (if n
      (recur r (drop n tubes) (conj ts (take n tubes)))
      ts)))

(defn- sf-calc-tube [dataset tube Tw]
  (let [py (:pyrometer dataset)
        em (or (:emissivity-override tube)
               (:emissivity-calculated tube)
               (:emissivity tube)
               (:emissivity dataset)
               (:tube-emissivity py))
        lam (:wavelength py)
        Tm (:raw-temp tube)
        F 1
        dT (calc-dT-corr Tm Tw lam em F)
        Tc (if dT (+ Tm dT))]
    (assoc tube :corrected-temp Tc)))

(defn sf-calc-Tc [dataset config]
  (sf-update-sides dataset
                   (fn [side ci si]
                     (let [pdtcs (get-in config [:sf-config :chambers ci
                                                 :peep-door-tube-count])
                           ts (sf-split-tubes (:tubes side) pdtcs)]
                       (->> (mapcat (fn [tubes Tw]
                                      (map #(sf-calc-tube dataset % Tw) tubes))
                                    ts
                                    (map :avg (:wall-temps side)))
                            vec
                            (assoc side :tubes))))))
