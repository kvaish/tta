(ns ht.util.service
  (:require [clojure.string :as str]
            [cljs.core.async :refer [<! put!]]
            [cljs-http.client :as http]
            [re-frame.core :as rf]
            [goog.date :as gdate]
            [ht.config :refer [config]]
            [ht.util.common :refer [dev-log]]
            [ht.util.auth :refer [parse-claims]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce api-map (atom {}))

(defn add-to-api-map [{:keys [root api]}]
  (let [api (->> api
                 (map (fn [[k uri]]
                        [k (str root uri)]))
                 (into {}))]
    (swap! api-map merge api)))

(defn init []
  (add-to-api-map
    {:root (:portal-uri @config)
     :api {:fetch-auth "/auth/token/fetch"
           :app-roles "/api/service/get-app-roles" ;; ?app-id=<app-id>
           :logout "/auth/logout"}})
  (add-to-api-map
   ;; resource files located at the same place as index.html
   ;; just use relative path, hence no root and no preceding /
   {:root nil
    :api {:translation "data/translations.json"}}))

(defn api-uri
  ([api-key]
   (get @api-map api-key))
  ([api-key params]
   (reduce (fn [uri [pkey pval]]
             (str/replace uri (str pkey) (str pval)))
           (get @api-map api-key)
           params)))

(defn run
  ":allow-cache? : whether to allow or prevent cache of GET requests
  by the browser. If false, an additional query parameter *timestamp* is
  added to the url to invalidate any chacheing by browser. If true, it is
  skipped. Only applicable for GET methods. Default value: *false*  
  If :on-success is given, :event-success is ignored!  
  If :on-failure is given, :event-failuer is ignored!"
  [{:keys [method api-key api-params data
           on-success on-failure
           evt-success evt-failure
           allow-cache? token? accept]
    :or {allow-cache? false
         token? true
         accept :json}}]
  (let [token @(rf/subscribe [:ht.app.subs/auth-token])
        uri (api-uri api-key api-params)
        headers (as-> (case accept
                        :edn {"Accept" "application/edn"}
                        :json {"Accept" "application/json"}) $
                      (if token?
                        (assoc $ "Authorization" (str "Token " token))))]
    (go
      (let [{:keys [status body]}
            (<! (method
                  uri
                  (cond-> (merge {:query-params {}
                                  :with-credentials? (not token?)
                                  :headers headers}
                                 data)
                          ;; add timestamp if get method with no cache
                          (and (= method http/get)
                               (not allow-cache?))
                          (assoc-in [:query-params :timestamp]
                                    (.valueOf (gdate/DateTime.))))))]
        (if (= 200 status)
          (do
            (dev-log ["success:" body])
            (if on-success
              (on-success body)
              (if evt-success
                (rf/dispatch (conj evt-success body)))))
          (let [body (if (map? body) body {:message body})
                res (assoc body :status status)]
            (dev-log ["failure!" res])
            (if on-failure
              (on-failure res)
              (if evt-failure
                (rf/dispatch (conj evt-failure res))))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; portal api

(defn fetch-auth [{:keys [evt-success evt-failure]}]
  (run {:method http/get
        :api-key :fetch-auth
        :on-success (fn [{:keys [token claims]}]
                      (rf/dispatch (conj evt-success token (parse-claims claims))))
        :evt-failure evt-failure
        :token? false
        :accept :edn}))

(defn logout [{:keys [evt-success evt-failure]}]
  (run {:method http/post
        :api-key :logout
        :evt-success evt-success
        :evt-failure evt-failure
        :token? false
        :accept :edn}))

(defn fetch-app-roles [{:keys [evt-success evt-failure]}]
  (run {:method http/get
        :api-key :app-roles
        :data {:query-params {:app-id (:app-id @config)}}
        :on-success (fn [{:keys [roles]}]
                      (rf/dispatch (conj evt-success roles)))
        :evt-failure evt-failure
        :token? false
        :accept :edn}))

(defn fetch-translation [{:keys [evt-success evt-failure]}]
  ;; repsonse format is json, hence do not use :edn
  (run {:method http/get
        :api-key :translation
        :evt-success evt-success
        :evt-failure evt-failure
        :token? false}))
