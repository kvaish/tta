(ns tta.config
  (:require [tta.util.interop :as i]))

(def debug?
  ^boolean goog.DEBUG)

(defonce config (atom {:app-id "truetemp"
                       :portal-uri nil}))


(defn init []
  (swap! config assoc
         :portal-uri (i/oget js/htAppConfig :portalUri)))
