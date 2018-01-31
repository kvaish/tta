(ns tta.schema.user
  (:require [ht.util.schema :as u]))

(def schema
  {:user {:id           u/id-field
          :client-id    "clientId"
          :plant-id     "plantId"
          :agreed?      "isAgreed"
          :email-alert? "ifEmailAlert"}})
