(ns ht.template
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))


(def files
  {:event
   ";; events for component my-comp
(ns tta.component.my-comp.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [tta.app.event :as app-event]))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:component :my-comp :data] data)))

(rf/reg-event-fx
 ::send-data
 (fn [{:keys [db]} [_ data]]
   {:db (assoc db :busy? true)
    ;; raise more side effects, such as
    ;; :my-fx [my-data]
    }))"

   :style
   ";; styles for component my-comp
(ns tta.component.my-comp.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [tta.app.style :as app-style
             :refer [color color-hex color-rgba]]))

(def my-comp {:color (color :royal-blue)
            ::stylefy/sub-styles
            {:item {:color (color :white)}}})"

   :subs
   ";; subscriptions for component my-comp
(ns tta.component.my-comp.subs
  (:require [re-frame.core :as rf]
            [tta.util.auth :as auth]
            [tta.app.subs :as app-subs]))

(rf/reg-sub
 ::data
 (fn [db _]
   (get-in db [:component :my-comp :data])))"

   :view
   ";; view elements component my-comp
(ns tta.component.my-comp.view
  (:require [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.util.common :refer [translate]]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.my-comp.style :as style]
            [tta.component.my-comp.subs :as subs]
            [tta.component.my-comp.event :as event]))

(defn my-comp [props]
  [:div (use-style style/my-comp)
   [:div (use-sub-style style/my-comp :item)
    \"my-comp\"]])"})


(defn create-comp [nc]
  (let [d (str "src/cljs/tta/component/"
               (str/replace nc "-" "_"))]
    (println "creating new folder " d)
    (if (io/make-parents (str d "/test"))
      (->> files
           (map (fn [[nf codes]]
                  (let [f (str d "/" (name nf) ".cljs")]
                    (spit f (str/replace codes "my-comp" nc))
                    (println "created " f)))))
      (println "couldn't create new folder. delete old folder if any."))))
