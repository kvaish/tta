(ns tta.schema.user)

(def schema
  (let [user {:id        "id"
              :client-id "clientId"
              :plant-id  "plantId"

              :agreed?      "isAgreed"
              :email-alert? "ifEmailAlert"}]

    {:user    user
     :db/user user}))
