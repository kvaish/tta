(ns tta.app.db
  (:require [tta.info :refer [about features operations]]))

(defonce default-db
  (atom
   {:about nil
    :features nil
    :operations nil
    :countries []
    ;; entities
    :user {:active nil
           :all {:user-id {:aggreed? false
                           :plant-id ""
                           :client-id ""}}}
    :client {:active nil
             :all {}}
    :plant {:active nil
            :all {}}
    
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
    {:user-agreement
     {:open? false}
     :choose-client
     {:open? false}
     :choose-plant
     {:open? false}}}))

(defn init []
  (swap! default-db
         (fn [db]
           (-> db
               (assoc :about about
                      :features features
                      :operations operations)))))
