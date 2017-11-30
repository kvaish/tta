(ns tta.app.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]))


(def ht-colors {;; haldor topsoe standard colors
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
                :brown        "#795548"
                })

(defn color
  ([color-key]
   (get ht-colors color-key))
  ([color-key pct-lighten]
   (-> (get ht-colors color-key)
       (gc/lighten pct-lighten))))

(defn color-hex
  ([color-key]
   (gc/as-hex
    (get ht-colors color-key)))
  ([color-key pct-lighten]
   (-> (get ht-colors color-key)
       (gc/lighten pct-lighten)
       (gc/as-hex))))


(def theme
  {:font-family "open_sans"
   :palette {:primary1Color (color-hex :royal-blue)
             :primary2Color (color-hex :ocean-blue)
             :primary3Color (color-hex :slate-grey 20)
             :accent1Color (color-hex :monet-pink)
             :accent2Color (color-hex :alumina-grey)
             :accent3Color (color-hex :slate-grey)
             ;; :textColor darkBlack
             ;; :alternateTextColor :white
             ;; :canvasColor :white
             :borderColor (color-hex :alumina-grey)
             ;; :disabledColor fade(darkBlack, 0.3)
             :pickerHeaderColor (color-hex :sky-blue)
             ;; :clockCircleColor fade(darkBlack, 0.7)
             ;; :shadowColor :fullBlack
             }})

(defn init []
  (stylefy/init)
  
  (stylefy/font-face
   {:font-family "open_sans"
    :src "url('./fonts/open-sans-light.woff') format('woff')"
    :font-weight 300
    :font-style "normal"})

  (stylefy/font-face
   {:font-family "open_sans"
    :src "url('./fonts/open-sans.woff') format('woff')"
    :font-weight 400
    :font-style "normal"})

  (stylefy/font-face
   {:font-family "open_sans"
    :src "url('./fonts/open-sans-bold.woff') format('woff')"
    :font-weight 700
    :font-style "normal"})

  (stylefy/font-face
   {:font-family "open_sans"
    :src "url('./fonts/open-sans-italic.woff') format('woff')"
    :font-weight 400
    :font-style "italic"})

  (stylefy/tag "body"
               {:margin 0
                :padding 0
                :font-family "open_sans"})

  :done)


