(ns tta.util.auth
  (:require [ht.util.auth :as ht-auth]
            [tta.info :as info]))

(defn allow-app? [claims]
  (and
   (ht-auth/allow-feature? claims :standard)
   (ht-auth/allow-operation? claims :view info/operations)))

(defn allow-root-content?
  [claims content-id]
  ;;TODO: apply access-rules
  (case content-id
    :home           true
    :dataset        true
    :trendline      true
    :settings       true
    :gold-cup       true
    :config-history true
    :config         true
    :logs           true
    ;; buttons
    :data-entry     true
    :import-logger  true
    :print-logsheet true
    false))
