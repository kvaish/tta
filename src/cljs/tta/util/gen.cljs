(ns tta.util.gen
  (:require [re-frame.core :as rf]
            [tta.util.interop :as i]))

(defn get-window-size []
  (js/console.log js/window.innerHeight)
  {:width (i/oget js/window :innerWidth)
   :height (i/oget js/window :innerHeight)})

(defn translate [key-v default]
  (or @(rf/subscribe (conj [:tta.app.subs/translate] key-v))
      default))
