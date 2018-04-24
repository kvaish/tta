(ns dev.load
  (:require [ht.user :refer [mount-workspace]]
            [tta.core :refer [mount-root]]))

(defn on-jsload []
  (mount-root)
  (mount-workspace))
