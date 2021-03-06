;; view elements component home
(ns tta.component.home.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.util.interop :as i]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.app.icon :as ic]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.component.home.style :as style]
            [tta.component.home.subs :as subs]
            [tta.component.home.event :as event]))

(defn as-left-icon [icon]
  (r/as-element [:span [icon {:style {:position "absolute"}}]]))

;; context menu to be shown in settings menu
(defn context-menu []
  (let [topsoe? @(rf/subscribe [::ht-subs/topsoe?])]
    [(if-not topsoe?
       {:id :license
        :icon (as-left-icon ic/license)
        :label-fn #(translate [:home :menu :license] "License")
        :event-id ::event/show-license})]))

(defn card [props]
  (let [{:keys [id primary? access on-select action
                title buttons icon desc]} props
        card-style (if primary? style/card-primary style/card-secondary)]
    [ui/paper (merge (use-style
                      (if (= access :enabled)
                        (if buttons card-style
                            (style/set-clickable card-style))
                        (style/disable-card card-style)))
                     (if (and (= access :enabled) (not buttons))
                       {:on-click (or action #(on-select id))}))
     [:div (use-sub-style card-style :title) title]
     [:hr (use-sub-style card-style :hr)]
     [:p (use-sub-style card-style :desc) desc]
     [:img (merge (use-sub-style card-style :icon) {:src icon})]]))

(defn card-with-buttons [props]
  (let [{:keys [access buttons]} props]
    (->
     [:div (use-style style/card-with-buttons)
      (card props)]
     (into
      (if buttons
        (map (fn [{:keys [label action] btn-access :access}]
               (let [enabled? (= :enabled access btn-access)]
                 [ui/paper
                  (merge (use-style (if enabled?
                                      (style/set-clickable style/card-button)
                                      (style/disable-card style/card-button)))
                         (if enabled? {:on-click action}))
                  [:span (use-sub-style style/card-button :title) label]]))
             buttons))))))

;; purpose of this component is to ensure load the draft from stroage
(defn draft-loader [has-draft?]
  (r/create-class
   {:reagent-render (fn [])
    :component-did-mount (fn [_]
                           (if-not has-draft?
                             (rf/dispatch [::event/load-draft])))}))

(defn home [props]
  (let [{:keys [on-select]} props
        access-rule @(rf/subscribe [::subs/access-rules])
        action-fn #(do
                     (i/ocall %1 :stopPropagation)
                     (rf/dispatch [%2]))
        has-draft? @(rf/subscribe [::subs/has-draft?])]
    [:div (use-style style/home)
     [draft-loader has-draft?]
     [:div (use-sub-style style/home :primary-row)
      (doall
       (->> [{:id :dataset
              :key :dataset-creator
              :primary? true
              :title (translate [:home-card :dataset-creator :title]
                                "Create Dataset")
              :buttons
              (map
               #(assoc % :access (get-in access-rule [:button (:id %)]))
               [{:id :data-entry
                 :label
                 (if has-draft?
                   (translate [:home :button :data-entry-continue]
                              "Continue Dataset")
                   (translate [:home :button :data-entry]
                              "Data Entry"))
                 :action #(action-fn % ::event/nav-data-entry)}
                {:id :import-logger
                 :label (translate [:home :button :import-logger]
                                   "Import from Logger App")
                 :action #(action-fn % ::event/nav-import-logger)}
                {:id :print-logsheet
                 :label (translate [:home :button :print-logsheet]
                                   "Print logsheet pdf")
                 :action #(action-fn % ::event/print-logsheet)}])
              :icon "images/create-dataset.svg"}
             {:id :dataset
              :primary? true
              :title (translate [:home-card :dataset-analyzer :title]
                                "Analyze Dataset")
              :desc (translate [:home-card :dataset-analyzer :description]
                               "Preview the Overall, TWT, Burner status of latest published dataset")
              :icon "images/analyze-dataset.svg"
              :on-select #(on-select :dataset {:mode :read})}
             {:id :trendline
              :primary? true
              :title (translate [:home-card :trendline :title]
                                "Trendline Graph")
              :desc (translate [:home-card :trendline :description]
                               "Overview of all the recorded datasets")
              :icon "images/trendline-graph.svg"}]
            (map (fn [{:keys [id] :as card}]
                   (-> card
                       (assoc :access (get-in access-rule [:card id]))
                       (update :on-select #(or % on-select)))))
            (remove #(= :hidden (:access %)))
            (map (fn [props]
                   ^{:key (or (:key props)
                              (:id props))}
                   [card-with-buttons props]))))]

     [:div (use-sub-style style/home :secondary-row)
      (doall
       (->>
        [{:id :settings
          :title (translate [:home-card :settings :title]
                            "Plant Settings")
          :desc (translate [:home-card :settings :description]
                           "Manage pyrometer, custom emissivity")
          :icon "images/plant-settings.svg"}
         {:id :gold-cup
          :title (translate [:home-card :gold-cup :title]
                            "GoldCup")
          :desc (translate [:home-card :gold-cup :description] "")
          :icon "images/goldcup.svg"
          :action #(rf/dispatch [::event/nav-gold-cup])}
         {:id :config
          :title (translate [:home-card :config :title]
                            "Configure Plant")
          :desc (translate [:home-card :config :description]
                           "")
          :icon "images/configure-plant.svg"}
         {:id :config-history
          :title (translate [:home-card :config-history :title]
                            "Reformer History")
          :desc (translate [:home-card :config-history :description]
                           "")
          :icon "images/reformer-history.svg"}
         {:id :logs
          :title (translate [:home-card :logs :title]
                            "Logs")
          :desc (translate [:home-card :logs :description]
                           "All deleted dataset logs that can be auto recovered")
          :icon "images/logs.svg"}]
        (map #(assoc % :access (get-in access-rule [:card (:id %)])
                     :on-select on-select))
        (remove #(= :hidden (:access %)))
        (map (fn [props]
               ^{:key (:id props)} [card props]))))]]))
