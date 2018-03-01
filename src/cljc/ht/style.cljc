(ns ht.style
  (:require [garden.color :refer [as-hsla as-hex as-rgb]]))

(def colors { ;; haldor topsoe standard colors
             :royal-blue   "#002856"
             :sky-blue     "#54c9e9"
             :ocean-blue   "#0048bb"
             :monet-pink   "#f55197"
             :bitumen-grey "#323c46"
             :slate-grey   "#7c868d"
             :alumina-grey "#d0d3d4"
             ;; material standard colors
             :white        "#ffffff"
             :black        "#000000"
             :red          "#f44336"
             :green        "#4caf50"
             :blue         "#2196f3"
             :amber        "#ffc107"
             ;; to be used rarely
             :pink         "#e91e63"
             :purple       "#9c27b0"
             :indigo       "#3f51b5"
             :cyan         "#00bcd4"
             :teal         "#009688"
             :lime         "#cddc39"
             :yellow       "#ffeb3b"
             :orange       "#ff9800"
             :brown        "#795548"})

(defn lighten [color pct]
  (let [c (as-hsla color)]
    (update c :lightness
            (fn [value]
              (+ value (if (pos? pct)
                         (* (- 100 value) (/ pct 100))
                         (/ (* value pct) 100)))))))

(defn color
  "get color suitable for use with stylefy and garden"
  ([color-key]
   (get colors color-key))
  ([color-key %-lighten]
   (-> (get colors color-key)
       (lighten %-lighten))))

(defn color-hex
  "get color as hex string"
  ([color-key]
   (as-hex
    (get colors color-key)))
  ([color-key %-lighten]
   (-> (get colors color-key)
       (lighten %-lighten)
       (as-hex))))

(defn color-rgba
  "get color as rgba() string"
  ([color-key]
   (color-rgba color-key nil nil))
  ([color-key %-lighten]
   (color-rgba color-key %-lighten nil))
  ([color-key %-lighten opacity]
   (let [{:keys [red green blue]}
         (as-rgb
          (if %-lighten
            (color color-key %-lighten)
            (color color-key)))
         opacity (or opacity 1)]
     (str "rgba(" red "," green "," blue "," opacity ")"))))


(def palettes
  {:chart [(:royal-blue colors)
           (:sky-blue colors)
           (:monet-pink colors)
           (:green colors)]})

(def root-layout
  {:head-row-height 66 ;; logo 18px + top/bottom: 24px = 66px
   :sub-head-row-height 44})

(def gradients
  {:blue-spot-light "radial-gradient(circle farthest-side at 70% 600%,rgba(84,201,233,1),rgba(0,72,187,1)150%)"})
