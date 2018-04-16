(ns ht.util.common
  (:require [cljs.core.async :refer [<! put! promise-chan alt! timeout]]
            [clojure.string :as str]
            [cljsjs.filesaverjs]
            [goog.date :as g]
            [goog.date.DateTime]
            [goog.date.Date]
            [goog.date.UtcDateTime]
            [ht.config :refer [config]]
            [ht.util.interop :as i])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defn pick-one
  "return the first one in collection for which pred is true. if
none matched, returns nil."
  [pred coll]
  (first (filter pred coll)))

(defn get-window-size []
  ;; (js/console.log js/window.innerHeight)
  {:width (i/oget js/window :innerWidth)
   :height (i/oget js/window :innerHeight)})

(defn- key->str [key]
  (cond
      (keyword? key) (name key)
      (string? key) key
      :error (throw "invalid key! must be a string or keyword.")))

(defn- get-storage-js
  "get from localStorage as a js Object"
  ([key]
   (get-storage-js key false))
  ([key common?]
   (let [key (if common? key
                 (str (:app-id @config) "_" key))]
     (-> (i/oget js/localStorage key)
         i/json-parse))))

(defn get-storage
  "get from localStorage as a clojure map"
  ([key]
   (get-storage key false))
  ([key common?]
   (js->clj (get-storage-js (key->str key) common?)
            :keywordize-keys true)))

(defn- set-storage-js
  "save to localStorage a js object"
  ([key value]
   (set-storage-js key value false))
  ([key value common?]
   (let [key (if common? key
                 (str (:app-id @config) "_" key))]
     (->> (i/json-str value)
          (i/oset js/localStorage key)))))

(defn set-storage
  "save to localStorage a clojure map.  
*key* should be a string or keyword"
  ([key value]
   (set-storage key value false))
  ([key value common?]
   (set-storage-js (key->str key) (clj->js value) common?)))

(defn dev? [] (= "dev" (i/oget js/htAppEnv :mode)))

(defn dev-log [& args]
  (if (dev?)
    (i/oapply js/console :log args)))


(defn add-event [el, event, handler]
  (if el
    (cond
      (i/oget el :attachEvent)
      (i/ocall el :attachEvent (str "on" event) handler)
      (i/oget el :addEventListener)
      (i/ocall el :addEventListener event handler true)
      :else
      (i/oset el (str "on" event) handler))))

(defn remove-event [el event handler]
  (if el
    (cond
      (i/oget el :detachEvent)
      (i/ocall el :detachEvent (str "on" event) handler)
      (i/oget el :removeEventListener)
      (i/ocall el :removeEventListener event handler true)
      :else
      (i/oset el (str "on" event) nil))))

(defn get-control-pos [e]
  (let [cpos (if-let [ts (i/oget e :touches)]
               (i/ocall ts :item 0)
               e)]
    {:page-x (i/oget cpos :pageX)
     :page-y (i/oget cpos :pageY)}))

(defn save-as [blob filename]
  (js/saveAs blob filename))

(defn save-to-file [filename content & [mime-type]]
  (let [mime-type (or mime-type (str "text/plain;charset=" (.-characterSet js/document)))
        blob (new js/Blob
                  (clj->js [content])
                  (clj->js {:type mime-type}))]
    (js/saveAs blob filename)))

(defn svg-string-2-image
  "converts the givent svg string to a PNG image blob for download purpose.
  Returns a promise chan, from which take [blob]"
  [svg-string width height]
  (let [ret-chan (promise-chan)]
    (js/svgString2Image svg-string width height #(put! ret-chan %&))
    ret-chan))

(defn save-svg-to-file
  "returns a channel from which will give either :timeout or :success"
  [filename svg-string width height wait-seconds]
  (let [ret-chan (promise-chan)
        result-chan (svg-string-2-image svg-string width height)
        ;; default 10 second timeout
        timeout-chan (timeout (* 1000 (or wait-seconds 10)))]
    (go
      (alt!
        timeout-chan (put! ret-chan :timeout)
        result-chan ([[blob]]
                     (js/saveAs blob filename)
                     (put! ret-chan :success))))
    ret-chan))

(defn tooltip-pos
  "checks where to put the tooltip around the anchor and
  returns tooltip position (x,y,top?,left?)  
  view top-left: vx xy (default: 0 0)  
  anchor rectangle: ax, ay, aw, ah  
  tooltip size: tw, th  
  align anchor and tooltip by x offset: adx, tdx  
  vertical margin between anchor and tooltip: tym  
  defaults: adx: middle of anchor, tdx: adx, tym: 0"
  [{:keys [vx vy
           ax ay aw ah adx
           tw th tdx tym]
    :or {tym 0, vx 0, vy 0}}]
  (let [adx (or adx (/ aw 2))
        tdx (or tdx adx)
        x (- (+ ax aw tdx) adx tw)
        [x left?] (if (<= vx x) [x true]
                      [(- (+ ax adx) tdx) false])
        y (- ay tym th)
        [y top?] (if (<= vy y) [y true]
                     [(+ ay ah tym) false])]
    {:x x, :y y, :top? top?, :left? left?}))

;; date time util ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn now-utc
  "returns current time with utc timezone.
  such as (js/Date. \"2018-04-05T20:32:22Z\") "
  []
  (js/Date.))

(defn today-utc
  "returns local date as js Date.
  such as (js/Date. \"2018-04-04T00:00:00Z\") "
  []
  (let [d (g/Date.)]
    (-> (g/UtcDateTime. (.getYear d) (.getMonth d) (.getDate d) 0 0 0)
        (.valueOf)
        (js/Date.))))

(defn today-as-map
  "returns local date as a map of day, month, year.
  NOTE: first day of month is 1 and first month of year is 1."
  []
  (let [d (g/Date.)]
    {:day (.getDate d)
     :month (inc (.getMonth d))
     :year (.getYear d)}))

(defn now-as-map
  "returns current local time as a map of day, month, year
  and hour, minute, second.
  NOTE: first day of month is 1 and first month of year is 1."
  []
  (let [dt (g/DateTime.)]
    {:day (.getDate dt)
     :month (inc (.getMonth dt))
     :year (.getYear dt)
     :hour (.getHours dt)
     :minute (.getMinutes dt)
     :second (.getSeconds dt)}))

(defn to-date-map
  "returns a map with day, month, year in local time zone.
  NOTE: first day of month is 1 and first month of year is 1.
  js-date is expected to be without time
  such as (js/Date. \"2018-04-04T00:00:00Z\") "
  [js-date]
  (let [d (g/UtcDateTime. js-date)]
    {:day (.getDate d)
     :month (inc (.getMonth d))
     :year (.getYear d)}))

(defn to-date-time-map
  "returns a map with day, month, year, hour, minute, second
  NOTE: first day of month is 1 and first month of year is 1.
  in local time zone."
  [js-date-time]
  (let [dt (g/DateTime. js-date-time)]
    {:day (.getDate dt)
     :month (inc (.getMonth dt))
     :year (.getYear dt)
     :hour (.getHours dt)
     :minute (.getMinutes dt)
     :second (.getSeconds dt)}))

(defn from-date-map
  "returns js Date in UTC timezone with date part only.
  such as (js/Date. \"2018-04-04T00:00:00Z\")
  NOTE: first day of month is 1 and first month of year is 1."
  [{:keys [day month year]}]
  (-> (g/UtcDateTime. year (dec month) day 0 0 0)
      (.valueOf)
      (js/Date.)))

(defn from-date-time-map
  "returns js Date in UTC timezone.
  such as (js/Date. \"2018-04-04T20:30:10Z\")
  NOTE: first day of month is 1 and first month of year is 1."
  [{:keys [day month year hour minute second]
    :or {hour 0, minute 0, second 0}}]
  (-> (g/DateTime. year (dec month) day hour minute second)
      (.valueOf)
      (js/Date.)))
