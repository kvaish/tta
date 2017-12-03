(ns tta.app.db
  (:require [tta.util.common :as u]))

(defonce default-db
  {:view-size (u/get-window-size)
   :busy? false
   :storage {}
   :language   {:options [{:id :en, :name "English"}
                          {:id :es, :name "Español"}
                          {:id :ru, :name "pусский"}]
                :active :en
                :translation {:en {:main {:language {:label "English"}}}
                              :es {:main {:language {:label "Español"}}}
                              :ru {:main {:language {:label "pусский"}}}}}
   :auth {:token nil, :claims {}}

   ;; entities
   :user {:active "demo@demo.com"
          :all {"demo@demo.com" {:name "Demo User"}}}
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

    :home {;; primary
           :dataset-creator {}
           :dataset-analyzer {}
           :trendline {}
           ;; secondary
           :settings {}
           :config-history {}
           :goldcup {}
           :config {}
           :logs {}}}
   
   :dialog
   {}
})
