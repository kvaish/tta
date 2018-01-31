(ns ht.util.interop
  (:require [clojure.string :as str]
            [goog.object :as g]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; JSON parse & stringify   ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn json-str [o]
  (.call (g/get js/JSON "stringify") js/JSON o))

(defn json-parse [s]
  (if (string? s)
    (.call (g/get js/JSON "parse") js/JSON s)))


;;;;;;;;;;;;;;;;;;;;;
;; object handling ;;
;;;;;;;;;;;;;;;;;;;;;

(defn oget [o k]
  (g/get o (name k)))

(defn oset [o k v]
  (g/set o (name k) v))

(defn oget-in [o ks]
  (let [ks (map name ks)]
    (g/getValueByKeys o (to-array ks))))

(defn oset-in [o ks v]
  (let [ks (map name ks)
        k (last ks)
        ks (not-empty (butlast ks))
        o (if-not ks o (g/getValueByKeys o (to-array ks)))]
    (if (object? o)
      (g/set o k v)
      (throw (js/Error. (if ks
                          (str "not an object at ." (str/join "." ks))
                          "not an object" ))))))

(defn oset-in+ [o ks v]
  (if-not (object? o)
    (js/Error. "not an object"))
  (loop [[k & ks] (map name ks)
         o o]
    (if (empty? ks)
      (g/set o k v)
      (let [oo (g/get o k)
            oo (if (object? oo) oo (js-obj))]
        (g/set o k oo)
        (recur ks oo)))))

(defn ocall [o f & args]
  (.apply (oget o f) o (to-array args)))

(defn oapply [o f & args]
  (let [args (concat (butlast args) (last args))]
    (.apply (oget o f) o (to-array args))))
