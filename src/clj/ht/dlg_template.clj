(ns ht.dlg-template
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))


(def files
  {:event
   ";; events for dialog my-dlg
(ns tta.dialog.my-dlg.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [day8.re-frame.forward-events-fx]
            [vimsical.re-frame.cofx.inject :as inject]
            [ht.app.event :as ht-event]
            [tta.app.event :as app-event]))

;; Do NOT use rf/subscribe
;; instead use cofx injection like [(inject-cofx ::inject/sub [::subs/data])]

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db [:dialog :my-dlg] merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db [:dialog :my-dlg] merge options {:open? false})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value]]
   (assoc-in db [:dialog :my-dlg :field id]
             {:valid? false
              :error nil
              :value value})))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :my-dlg :data] data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :my-dlg] merge options)))"

   :style
   ";; styles for dialog my-dlg
(ns tta.dialog.my-dlg.style
  (:require [stylefy.core :as stylefy]
            [garden.color :as gc]
            [garden.units :refer [px]]
            [ht.style :as ht :refer [color color-hex color-rgba]]
            [ht.app.style :as ht-style :refer [vendors]]
            [tta.app.style :as app-style]))
"

   :subs
   ";; subscriptions for dialog my-dlg
(ns tta.dialog.my-dlg.subs
  (:require [re-frame.core :as rf]
            [ht.app.subs :as ht-subs :refer [translate]]
            [tta.app.subs :as app-subs]))

;; Do NOT use rf/subscribe
;; instead add input signals like :<- [::query-id]
;; or use reaction or make-reaction (refer reagent docs)

;; primary signals
(rf/reg-sub
 ::dialog
 (fn [db _] (get-in db [:dialog :my-dlg])))


;;derived signals/subscriptions
(rf/reg-sub
 ::open?
 :<- [::dialog]
 (fn [dialog _]
   (:open? dialog)))

(rf/reg-sub
 ::data
 :<- [::dialog]
 (fn [dialog _]
   (:data dialog)))

(rf/reg-sub
 ::field
 :<- [::dialog]
 (fn [dialog [_ id]]
   (get-in dialog [:field id])))"


   :view
   ";; view elements dialog my-dlg
(ns tta.dialog.my-dlg.view
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
            [tta.dialog.my-dlg.style :as style]
            [tta.dialog.my-dlg.subs :as subs]
            [tta.dialog.my-dlg.event :as event]))

(defn my-dlg []
  (let [open? @(rf/subscribe [::subs/open?])]
    [ui/dialog
     {:open open?
      :modal true}
     [:div
      \"my-dlg\"]]))"

   })


(defn create-dlg [nd]
  (let [d (str "src/cljs/tta/dialog/"
               (str/replace nd "-" "_"))]
    (println "creating new folder " d)
    (if (io/make-parents (str d "/test"))
      (->> files
           (map (fn [[nf codes]]
                  (let [f (str d "/" (name nf) ".cljs")]
                    (spit f (str/replace codes "my-dlg" nd))
                    (println "created " f)))))
      (println "couldn't create new folder. delete old folder if any."))))
