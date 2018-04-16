(ns tta.app.db
  (:require [tta.info :refer [about features operations]]))

(defonce default-db
  (atom
   {:about nil
    :features nil
    :operations nil
    :countries []

    ;; entities
    :user nil
    :client nil
    :plant nil

    :component
    {:root {:header {}
            :sub-header {}
            :content {:active :home}}
     :home {}

     ;; primary
     :dataset {}
     :trendline {}

     ;; secondary
     :settings {}
     :config-history {}
     :gold-cup {}
     :config {}
     :logs {}}

    :dialog
    {:user-agreement
     {:open? false}
     :choose-client
     {:open? false}
     :choose-plant
     {:open? false}
     :edit-pyrometer
     {:open? false}
     :custom-emissivity
     {:open? false}}}))

(defn init []
  (swap! default-db
         (fn [db]
           (-> db
               (assoc :about about
                      :features features
                      :operations operations)))))
