(ns ht.setup
  (:require [re-frisk.core :refer [enable-re-frisk!]]
            [ht.util.interop :as i]))


(defn dev-setup []
  (enable-console-print!)
  (enable-re-frisk!)
  (i/oset js/htAppEnv :mode "dev")
  (println "dev mode"))


;; dev env specific initializations
(defn init []
  (dev-setup))
