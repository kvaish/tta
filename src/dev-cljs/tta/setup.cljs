(ns tta.setup)


(defn dev-setup []
  (enable-console-print!)
  (println "dev mode"))


;; dev env specific initializations
(defn init []
  (dev-setup))

