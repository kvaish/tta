(ns ht.style)


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
