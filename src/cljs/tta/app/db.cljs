(ns tta.app.db
  (:require [tta.info :refer [about features operations]]))

(defonce default-db
  (atom
   {:about nil
    :features nil
    :operations nil
    
    ;; entities
    :user {:active nil
           :all {:user-id {:info {:name ""}
                           :settings {:plant-id ""
                                      :client-id ""}}}}
    :client {:active "demo"
             :all {"demo" {:name "DemoCom"}}}
    :plant {:active "demo"
            :all {"demo" {:name "DemoPlant"}}}
    
    :dataset {:pools []
              :messages []}
    
    :component
    {:root {:header {}
            :sub-header {}
            :content {:active :home}}
     :home {}

     ;; primary
     :dataset-creator {}
     :dataset-analyzer {}
     :trendline {}

     ;; secondary
     :settings {}
     :config-history {}
     :goldcup {}
     :config {}
     :logs {}}
    
    :dialog
    {}}))

(defn init []
  (swap! default-db
         (fn [db]
           (-> db
               (assoc :about about
                      :features features
                      :operations operations)))))
