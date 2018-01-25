(ns tta.util.auth
  (:require [ht.util.auth :as ht-auth]
            [tta.info :as info]))

(defn allow-app? [claims]
  (and
   (ht-auth/allow-feature? claims :standard)
   (ht-auth/allow-operation? claims :view info/operations)))

(defn allow-root-content?
  [claims content]
  ;; TODO: implement checks
  true)
