(ns tta.util.service
  (:require [clojure.string :as str]
            [cljs.core.async :refer [<! put!]]
            [cljs-http.client :as http]
            [re-frame.core :as rf]
            [ht.config :refer [config]]
            [ht.util.interop :as i]
            [ht.util.service :refer [add-to-api-map api-uri]])
  (:require-macros [cljs.core.async.macros :refer [go]]))

(defonce init-once
  (add-to-api-map
   {:service {:root (:service-uri @config)
              :api {:fetch-user-settings "/user/:id"}}}))

(defn fetch-user-settings [{:keys [user-id evt-success evt-failure]}]
  )
