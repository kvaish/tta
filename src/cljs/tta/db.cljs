(ns tta.db
  (:require [tta.util.gen :as u]))

(defonce default-db
  {:view {:size (u/get-window-size)
          :root {:top-bar {}
                 :menu-bar {}
                 :main-container {:active :home}}}
   :language {:options [{:id :en, :name "English"}
                        {:id :ru, :name "pусский"}
                        {:id :es, :name "Español"}]
              :active :en
              :translation {:en {:main {:greet {:label "Hello"}}}
                            :es {:main {:greet {:label "Hola"}}}
                            :ru {:main {:greet {:label "Gutentag"}}}}}})
