(ns ht.app.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [ht.style :as ht :refer [color color-hex color-rgba]]))

(def vendors ["webkit" "ms" "moz" "o"])

(def theme
  {:font-family "open_sans"
   :palette {:primary1Color (color-hex :sky-blue #_:royal-blue)
             :primary2Color (color-hex :ocean-blue)
             :primary3Color (color-hex :alumina-grey #_:slate-grey 20)
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
                :font-family "open_sans"
                :overflow "hidden"})

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

(def app-fault
  {:title {:color (color :red)}
   :icon {:color (color :red)
          :font-size "28px"
          :margin-right "24px"}})

(defn message-box [level]
  {:icon {:color (color (case level
                          :error :red
                          :warning :amber
                          :green))
          :font-size "28px"
          :margin-right "24px"}})

(def icon-plain {:fill "none !important"
                 :stroke (color-rgba :black 0 0.87)
                 :stroke-width 0.5})

(def optional-dialog-head
  {:position "relative"
   :margin 0
   :padding 0
   ::stylefy/sub-styles
   {:title {:display "block"}
    :close {:position "absolute !important"
            :top 0
            :right 0}}})
