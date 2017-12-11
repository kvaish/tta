(ns ht.app.db
  (:require [ht.util.common :as u]
            [ht.config :refer [config]]))

(defonce default-db
  (atom
   {:about nil
    :features nil
    :operations nil
    :config {}
    :view-size {:width 1024, :height 768}
    :busy? false
    :storage {}
    :language {:options []
               :active :en
               :translation {:en {:main {:language {:label "English"}}}
                             :es {:main {:language {:label "Español"}}}
                             :ru {:main {:language {:label "pусский"}}}}}
    :auth {:token nil, :claims nil}
    :component {}
    :dialog {}}))


(defn init []
  (swap! default-db
         (fn [db]
           (-> db
            (assoc :view-size (u/get-window-size))
            (assoc :config @config)
            (assoc-in [:language :options]
                      (mapv (fn [{:keys [code flag name]}]
                                {:id (keyword code)
                                 :name name})
                              (:languages @config)))))))
