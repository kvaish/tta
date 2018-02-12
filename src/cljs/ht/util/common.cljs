(ns ht.util.common
  (:require [cljs.core.async :refer [<! put! promise-chan alt! timeout]]
            [clojure.string :as str]
            [cljsjs.filesaverjs]
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
               (first ts)
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
