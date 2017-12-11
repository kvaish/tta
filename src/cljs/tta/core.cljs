(ns tta.core
  (:require [ht.core :as ht]
            [tta.app.db :as db]
            [tta.app.cofx] ;; ensure load
            [tta.app.fx]   ;; ensure load
            [tta.app.view] ;; ensure load
            [tta.component.root.view :refer [root]]))

(js/console.log "!VERSION!")

(def mount-root (ht/create-root-mounter root))

(defn ^:export init []
  (ht/init)
  (db/init)
  (ht/init-db @db/default-db)
  (mount-root))
