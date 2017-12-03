(ns tta.app.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [ht.style :as ht]))

(defn color
  "get color suitable for use with stylefy and garden"
  ([color-key]
   (get ht/colors color-key))
  ([color-key %-lighten]
   (-> (get ht/colors color-key)
       (gc/scale-lightness %-lighten))))

(defn color-hex
  "get color as hex string"
  ([color-key]
   (gc/as-hex
    (get ht/colors color-key)))
  ([color-key %-lighten]
   (-> (get ht/colors color-key)
       (gc/scale-lightness %-lighten)
       (gc/as-hex))))

(defn color-rgba
  "get color as rgba() string"
  ([color-key]
   (color-rgba color-key nil nil))
  ([color-key %-lighten]
   (color-rgba color-key %-lighten nil))
  ([color-key %-lighten opacity]
   (let [{:keys [red green blue]}
         (gc/as-rgb
          (if %-lighten
            (color color-key %-lighten)
            (color color-key)))
         opacity (or opacity 1)]
     (str "rgba(" red "," green "," blue "," opacity ")"))))

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


(def busy-screen
  {:content {:width "130px"}
   :paper {:background "none"
           :box-shadow "none"}
   :spinner {:width "100px"
               :height "100px"
               :background-image "url(images/hexagon_spinner.gif)"
               :background-repeat "no-repeat"
               :background-size "contain"}})
