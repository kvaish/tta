(ns ht.app.db
  (:require [ht.util.common :as u]
            [ht.config :refer [config]]))

(defonce default-db
  (atom
   {:about nil
    :roles nil
    :features nil
    :operations nil
    :config {}
    :view-size {:width 1024, :height 768}
    :busy? false
    :service-failure nil
    :storage {}
    :language {:options []
               :active :en
               :translation {:en {:main {:language {:label "English"}}}
                             :es {:main {:language {:label "Español"}}}
                             :ru {:main {:language {:label "pусский"}}}}}
    :auth {:token nil, :claims nil, :fetched? false}
    :component {}
    :dialog {}}))


(defn init []
  (let [language-options (mapv (fn [{:keys [code _ name]}]
                                 {:id (keyword code)
                                  :name name})
                               (:languages @config))
        language-active (-> (u/get-storage :language true)
                            (:code)
                            (keyword)
                            ((set (map :id language-options)))
                            (or :en))]
    (swap! default-db
           (fn [db]
             (-> db
                 (assoc :view-size (u/get-window-size))
                 (assoc :config @config)
                 (update :language merge {:options language-options
                                          :active language-active}))))))
