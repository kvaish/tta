(ns ht.comp-template
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))


(def files
  {:event
   ";; events for component my-comp
(ns tta.component.my-comp.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

;; Add some event handlers, like
#_ (rf/reg-event-db
    ::event-id
    (fn [db [_ param]]
      (assoc db :param param)))
;;
;; NOTE: all event handler functions should be pure functions
;; Typically rf/reg-event-db should suffice for most cases, which
;; means you should not access or modify any global vars or make
;; external service calls.
;; If external data/changes needed use rf/reg-event-fx, in which case
;; your event handler function should take a co-effects map and return
;; a effects map, like
#_ (rf/reg-event-fx
    ::event-id
    (fn [{:keys[db]} [_ param]]
      {:db (assoc db :param param)}))
;;
;; If there is a need for external data then inject them using inject-cofx
;; and register your external data sourcing in cofx.cljs
;; Similarly, if your changes are not limited to the db, then use
;; rf/reg-event-fx and register your external changes as effects in fx.cljs
"

   :style
   ";; styles for component my-comp
(ns tta.component.my-comp.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht]
            [ht.app.style :as ht-style
             :refer [color color-hex color-rgba vendors]]
            [tta.app.style :as app-style]))

"

   :subs
   ";; subscriptions for component my-comp
(ns tta.component.my-comp.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]
            [tta.util.auth :as auth]))

;; primary signals
(rf/reg-sub
 ::my-comp
 (fn [db _]
   (get-in db [:component :my-comp])))

;; derived signals/subscriptions
"

   :view
   ";; view elements component my-comp
(ns tta.component.my-comp.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.my-comp.style :as style]
            [tta.component.my-comp.subs :as subs]
            [tta.component.my-comp.event :as event]))

(defn my-comp [props]
  [:div (use-style style/my-comp)
   \"my-comp\"])"})


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
