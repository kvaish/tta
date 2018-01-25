(ns ht.util.auth
  (:require [ht.config :refer [config]]))

(defn- in? [item coll]
  (if (and (coll? coll)
           (some #(= item %) coll))
    true
    false))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;; claims string format as received from portal                ;;;;
;;;; --------------------------------------------                ;;;;
;;;; {:id <user email>                                           ;;;;
;;;;  :name <user name>                                          ;;;;
;;;;  :isTopsoe <is topsoe user?>                                ;;;;
;;;;  :isEmailVerified <is email verfied?>                       ;;;;
;;;;  :isAdmin <is portal admin?>                                ;;;;
;;;;  :clientId <client id for external user>                    ;;;;
;;;;  :isClientAdmin <is client admin?>                          ;;;;
;;;;  :apps {<app id> {:features [<feature id>...]               ;;;;
;;;;                   :operations [<operation id>...]           ;;;;
;;;;                   :isAdmin <is app admin? developer>        ;;;;
;;;;                   :isOwner <is app owner? business owner>}} ;;;;
;;;;  :exp <expiry time>}                                        ;;;;
;;;;                                                             ;;;;
;;;; claims map after parsing                                    ;;;;
;;;; ------------------------                                    ;;;;
;;;; {:id <user email>                                           ;;;;
;;;;  :name            <user name>                               ;;;;
;;;;  :topsoe?         <is topsoe user?>                         ;;;;
;;;;  :email-verified? <is email verified?>                      ;;;;
;;;;  :admin?          <is portal admin?>                        ;;;;
;;;;  :client-id       <client id for external user>             ;;;;
;;;;  :client-admin?   <is client admin?>                        ;;;;
;;;;  :features        [<feature id keyword>...]                 ;;;;
;;;;  :operations      [<operation id keyword>...]               ;;;;
;;;;  :app-admin?      <is app admin?>                           ;;;;
;;;;  :app-owner?      <is app owner?>                           ;;;;
;;;;  :exp             <expiry time>}                            ;;;;
;;;;                                                             ;;;;
;;;; NOTE: false or nil values are omitted for brevity           ;;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn parse-claims [c]
  (if c
    (let [app-id (:app-id @config)
          app (get-in c [:apps app-id])
          fs  (not-empty (mapv keyword (:features app)))
          ops (not-empty (mapv keyword (:operations app)))]
      (cond-> {:id        (:id c)
               :name      (:name c)
               :client-id (:clientId c)
               :exp       (js/Date. (:exp c))}
        (:isTopsoe c)        (assoc :topsoe? true)
        (:isEmailVerified c) (assoc :email-verified? true)
        (:isAdmin c)         (assoc :admin? true)
        (:isClientAdmin c)   (assoc :client-admin? true)
        fs                   (assoc :features fs)
        ops                  (assoc :operations ops)
        (:isAdmin app)       (assoc :app-admin? true)
        (:isOwner app)       (assoc :app-owner? true)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; basic feature and operation check helpers

(defn allow-feature?
  "check if given feature (keyword) is allowed for given claims"
  [claims feature]
  (and claims
       (or (:topsoe? claims) ;; featuer restriction not applicable to Topsoe users
           (in? feature (:features claims)))))

(defn allow-operation?
  "check if given operation (keyword) is allowed for given claims"
  [claims operation app-operations]
  (and claims
       (or ((some-fn :admin? :app-admin? :app-owner?) claims)
           (and (:client-admin? claims)
                (not (get-in app-operations [operation :internal?])))
           (in? operation (:operations claims)))))
