;; events for dialog choose-client
(ns tta.dialog.choose-client.event
  (:require [re-frame.core :as rf]
            [re-frame.cofx :refer [inject-cofx]]
            [ht.app.event :as ht-event]
            [ht.util.common :refer [dev-log]]
            [tta.app.event :as app-event]))

(defonce ^:private state (atom {:counter 0}))

(defn- next-id []
  (:counter (swap! state update :counter inc)))

(defn- active-id? [id]
  (= id (:counter @state)))

(rf/reg-event-db
 ::open
 (fn [db [_ options]]
   (update-in db [:dialog :choose-client] merge options {:open? true})))

(rf/reg-event-db
 ::close
 (fn [db [_ options]]
   (update-in db [:dialog :choose-client] merge options {:open? false})))

(rf/reg-event-db
 ::set-field
 (fn [db [_ id value]]
   (assoc-in db [:dialog :choose-client :field id] {:valid? true
                                                    :error nil
                                                    :value value})))

(rf/reg-event-db
 ::set-data
 (fn [db [_ data]]
   (assoc-in db [:dialog :choose-client :data] data)))

(rf/reg-event-db
 ::set-options
 (fn [db [_ options]]
   (update-in db [:dialog :choose-client] merge options)))

(rf/reg-event-fx
 ::update-query
 (fn [_ [_ field-id value]]
   {:dispatch [::set-field field-id value]
    :dispatch-later [{:ms 500
                      :dispatch [::search-clients (next-id)]}]}))

(rf/reg-event-fx
 ::search-clients
 (fn [{:keys [db]} [_ id]]
   (if (active-id? id)
     (let [fields (get-in db [:dialog :choose-client :field])
           query (->> [:name :short-name :location :country :plant?]
                      (map (fn [fid]
                             [fid
                              (let [v (get-in fields [fid :value])]
                                (if (string? v) (not-empty v) v))]))
                      (remove #(nil? (second %)))
                      (into {:skip 0, :limit 10}))]
       ;; ensure at least 3 letters in one of name, short-name or location
       (if (some #(> (count (% query)) 2)
                 [:name :short-name :location])
         {:db (assoc-in db [:dialog :choose-client :data :busy?] true)
          :service/search-clients {:query query
                                   :evt-success [::set-clients id query]}})))))

(rf/reg-event-db
 ::set-clients
 (fn [db [_ id query clients]]
   (if-not (active-id? id)
     db ;; no action
     (let [{:keys [limit]} query
           more? (= (count clients) limit)]
       (update-in db [:dialog :choose-client :data]
                  #(if (= id (:id %))
                     ;; append
                     (-> (assoc % :more? more?, :busy? false, :query query)
                         (update :clients into clients))
                     ;; reset
                     {:id id
                      :more? more?, :busy? false, :query query
                      :clients clients}))))))

(rf/reg-event-fx
 ::search-more-clients
 (fn [{:keys [db]} _]
   (let [{:keys [query id more?]} (get-in db [:dialog :choose-client :data])
         query (update query :skip + (:limit query))]
     (if (and (active-id? id) more?)
       {:db (assoc-in db [:dialog :choose-client :data :busy?] true)
        :service/search-clients {:query query
                                 :evt-success [::set-clients id query]}}))))

(rf/reg-event-fx
 ::select-client
 (fn [db [_ client]]
   (dev-log "selected client id: " (:id client))
   {:dispatch-n (list [::close]
                      [:tta.dialog.choose-plant.event/open {:data {:client client}}])}))
