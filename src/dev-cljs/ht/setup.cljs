(ns ht.setup
  (:require [re-frisk.core :refer [enable-re-frisk!]]))


(defn dev-setup []
  (enable-console-print!)
  (enable-re-frisk!)
  (println "dev mode"))


;; dev env specific initializations
(defn init []
  (dev-setup))

