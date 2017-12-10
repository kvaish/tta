(ns ht.app.subs
  (:require [re-frame.core :as rf]))

;;;;;;;;;;;;;;;;;;;;;
;; Primary signals ;;
;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::about-app
 (fn [db _] (:about db)))

(rf/reg-sub
 ::app-features
 (fn [db _] (:features db)))

(rf/reg-sub
 ::app-operations
 (fn [db _] (:operations db)))

(rf/reg-sub
 ::config
 (fn [db _] (:config db)))

(rf/reg-sub
 ::view-size
 (fn [db _] (:view-size db)))

(rf/reg-sub
 ::busy?
 (fn [db _] (:busy? db)))

(rf/reg-sub
 ::storage
 (fn [db _] (:storage db)))

(rf/reg-sub
 ::language
 (fn [db _] (:language db)))

(rf/reg-sub
 ::auth
 (fn [db _] (:auth db)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Derived signals/subscriptions ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::language-options
 :<- [::language]
 (fn [language _] (:options language)))

(rf/reg-sub
 ::active-language
 :<- [::language]
 (fn [db _] (:active language)))

(rf/reg-sub
 ::translation
 :<- [::language]
 :<- [::active-language]
 (fn [[language active-language] [_ key-v]]
   (-> (:translation language)
       (get active-language)
       (get-in key-v))))

(defn translate
  "helper function to subscribe translation in view"
  [key-v default]
  (or @(rf/subscribe (conj [::translation] key-v))
      default))

(rf/reg-sub
 ::auth-token
 :<- [::auth]
 (fn [auth _] (:token auth)))

(rf/reg-sub
 ::auth-claims
 :<- [::auth]
 :<- [::config]
 (fn [[auth config] _]
   (as-> (:claims auth) $
     (assoc $ :app
            (-> (get-in $ [:apps (:app-id config)])
                (update :features #(mapv keyword %))
                (update :operations #(mapv keyword %))))
     (dissoc $ :apps))))

(rf/reg-sub
 ::features
 :<- [::auth-claims]
 :<- [::app-features]
 (fn [[claims app-features] _]
   (->> (if (:isTopsoe claims)
          app-features ;; internal user gets all features
          ;; external user gets only subscribed ones
          (filter (comp (set (get-in claims [:app :features])) :id)
                  app-features))
        ;; arrange in a map by feature id
        (reduce (fn [fs {:keys [id] :as f}]
                  (assoc fs id f))
                {}))))

(rf/reg-sub
 ::operations
 :<- [::auth-claims]
 :<- [::app-operations]
 (fn [[claims app-operations] _]
   (->>
    (cond
      ;; client admin gets all non-internal operations
      (:isClientAdmin claims) (remove :internal? app-operations)
      ;; admin and owners get all operations
      (or
       (:isAdmin claims)
       (get-in claims [:app :isOwner])
       (get-in claims [:app :isAdmin]))
      app-operations
      ;; others get only specified operations
      :others
      (filter (comp (set (get-in claims [:app :operations])) :id)
              (if (:isTopsoe claims)
                app-operations
                (remove :internal? app-operations))))
    ;; arrange in a map by operation id
    (reduce (fn [ops {:keys [id] :as op}]
              (assoc ops id op))
            {}))))



