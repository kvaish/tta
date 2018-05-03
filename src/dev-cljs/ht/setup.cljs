(ns ht.setup
  (:require [ht.util.interop :as i]
            ;; [re-frisk-remote.core :refer [enable-re-frisk-remote!]]
            [re-frisk.core :refer [enable-re-frisk!]]))


(defn dev-setup []
  (enable-console-print!)
  (enable-re-frisk!) ;; standalone, re-frisk in the same page
  ;; (enable-re-frisk-remote!) ;; for use with "lein re-frisk"
  ;; (enable-re-frisk-remote! {:host "htx9a:4567", :enable-re-frame-10x? true})
  (i/oset js/htAppEnv :mode "dev")
  (println "dev mode"))


;; dev env specific initializations
(defn init []
  (dev-setup))
