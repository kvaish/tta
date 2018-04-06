(ns ht.app.subs
  (:require [re-frame.core :as rf]
            [reagent.ratom :refer [reaction]]
            [clojure.string :as str]))

;;;;;;;;;;;;;;;;;;;;;
;; Primary signals ;;
;;;;;;;;;;;;;;;;;;;;;

(rf/reg-sub
 ::about-app
 (fn [db _] (:about db)))

(rf/reg-sub
 ::app-roles
 (fn [db _] (:roles db)))

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

(rf/reg-sub
 ::service-failure
 (fn [db _] (:service-failure db)))

(rf/reg-sub
 ::message-box
 (fn [db _] (:message-box db)))

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
 (fn [language _] (:active language)))

(rf/reg-sub
 ::translation
 :<- [::language]
 :<- [::active-language]
 (fn [[language active-language] [_ key-v]]
   (-> (:translation language)
       (get active-language)
       (get-in key-v))))

;; helper for translation ;;

(defn translate
  "helper function to subscribe translation in view"
  ([key-v default]
   (or @(rf/subscribe (conj [::translation] key-v))
       default))
  ([key-v default params]
   (let [s (translate key-v default)]
     (reduce-kv (fn [s k v]
                  (str/replace s (str "{" (name k) "}") v))
                s params))))

;; auth derived signals ;;

(rf/reg-sub
 ::auth-token
 :<- [::auth]
 (fn [auth _] (:token auth)))

;; auth-claims - returns nil until fetched, then false or claims
(rf/reg-sub
 ::auth-claims
 :<- [::auth]
 :<- [::config]
 (fn [[auth config] _]
   (if (:fetched? auth)
     (if-let [claims (:claims auth)]
       (as-> claims $
         (assoc $ :app
                (-> (get-in $ [:apps (:app-id config)])
                    (update :features #(mapv keyword %))
                    (update :operations #(mapv keyword %))))
         (dissoc $ :apps))
       false))))

(rf/reg-sub
 ::features
 :<- [::auth-claims]
 :<- [::app-features]
 (fn [[claims app-features] _]
   (if claims
     (->> (if (:isTopsoe claims)
            app-features ;; internal user gets all features
            ;; external user gets only subscribed ones
            (filter (comp (set (get-in claims [:app :features])) :id)
                    app-features))
          ;; arrange in a map by feature id
          (reduce (fn [fs {:keys [id] :as f}]
                    (assoc fs id f))
                  {})))))

(rf/reg-sub
 ::operations
 :<- [::auth-claims]
 :<- [::app-operations]
 (fn [[claims app-operations] _]
   (if claims
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
              {})))))

(rf/reg-sub
 ::topsoe?
 :<- [::auth-claims]
 (fn [claims _] (:topsoe? claims)))

(rf/reg-sub
 ::roles ;; list of all roles
 :<- [::app-roles]
 :<- [::active-language]
 (fn [[roles lang] _]
   (->> roles
        (map (fn [[id role]]
               (let [id (name id)
                     n (or (get-in role [:about lang :name])
                           (get-in role [:about :en :name])
                           id)
                     d (or (get-in role [:about lang :description])
                           (get-in role [:about :en :description]))]
                 {:id id
                  :internal? (:isInternal role)
                  :name n
                  :description d}))))))

(rf/reg-sub
 ::user-roles ;; list of roles for current user
 :<- [::roles]
 :<- [::auth-claims]
 (fn [[roles claims] _]
   (let [admin? (some-fn :admin? :app-admin? :app-owner?)]
     (cond
       (admin? claims) roles
       (:client-admin? claims) (remove :internal? roles)
       :default
       (let [chk (set (:roles claims))]
         (filter #(-> % :id keyword chk) roles))))))
