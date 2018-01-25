(ns ht.exports
  (:require [clojure.data.json :as json]
            [tta.info :as info]))

(defn get-info []
  {:about info/about
   :features (mapv #(update % :id name) info/features)
   :operations (mapv #(as-> % $
                          (update $ :id name)
                          (if-not (:internal? $) $
                                  (assoc $ :isInternal true))
                          (dissoc $ :internal?))
                     info/operations)})

(defn export-info []
  (->>
   (get-info)
   (json/write-str)
   (spit "resources/public/data/app-info.json")))

(defn -main [& args]
  (export-info)
  (println "exported app-info.json"))
