(ns ht.app.db
  (:require [ht.util.common :as u]
            [ht.config :refer [config]]))

(defonce default-db
  {:about nil
   :features nil
   :operations nil
   :config @config
   :view-size (u/get-window-size)
   :busy? false
   :storage {}
   :language {:options (:language-options @config)
              :active :en
              :translation {:en {:main {:language {:label "English"}}}
                            :es {:main {:language {:label "Español"}}}
                            :ru {:main {:language {:label "pусский"}}}}}
   :auth {:token nil, :claims nil}
   :component {}
   :dialog {}})


