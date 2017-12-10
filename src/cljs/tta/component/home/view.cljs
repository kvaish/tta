;; view elements component home
(ns tta.component.home.view
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
            [tta.component.home.style :as style]
            [tta.component.home.subs :as subs]
            [tta.component.home.event :as event]))

(defn card [props]
  (let [{:keys [id primary? access on-select title buttons icon desc]} props
        card-style (if primary? style/card-primary style/card-secondary)]
    (case access
      (:enabled :disabled)
      [ui/paper (merge (use-style
                        (if (= access :enabled)
                          (style/set-clickable card-style true #_(not buttons))
                          (style/disable-card card-style)))
                       (if (and (= access :enabled)
                                true #_(not buttons))
                         {:on-click #(on-select id)}))
       [:div (use-sub-style card-style :title)
        title]
       [:hr (use-sub-style card-style :hr)]
       [:br]
       (if buttons
         (doall
          (map (fn [{:keys [id label action] btn-access :access}]
                 (let [disabled? (not (= :enabled access btn-access))
                       label-style (:label style/card-button)]
                   ^{:key id}
                   [ui/flat-button
                    {:label label
                     :disabled disabled?
                     :on-click action
                     :label-style (if disabled?
                                    (style/disable-button label-style)
                                      label-style)
                     :icon (r/as-element
                            [ui/font-icon {:class-name "fa fa-arrow-right"}])
                     :style (:root style/card-button)}]))
               buttons)))
       [:p (use-sub-style card-style :desc)
        desc]
       (if primary? [:i (use-sub-style card-style :icon
                                       {::stylefy/with-classes ["fa" "fa-home" "fa-5x"]})])]
      :hidden [:div {:style {:flex 1}}])))

(defn home [props]
  (let [{:keys [on-select]} props
        access-rule @(rf/subscribe [::subs/access-rules])]
    [:div (use-style style/home)
     [:div (use-sub-style style/home :primary-row)
      (doall
       (->> [{:id :dataset-creator
              :primary? true
              :title (translate [:home-card :dataset-creator :title]
                                "Create Dataset")
              :buttons
              (map
               #(assoc % :access (get-in access-rule [:button (:id %)]))
               [{:id :data-entry
                 :label (translate [:home :button :data-entry]
                                   "Data Entry")
                 :action #(rf/dispatch [::event/nav-data-entry])}
                {:id :import-logger
                 :label (translate [:home :button :import-logger]
                                   "Import from Logger App")
                 :action #(rf/dispatch [::event/nav-import-logger])}
                {:id :print-logsheet
                 :label (translate [:home :button :print-logsheet]
                                   "Print logsheet pdf")
                 :action #(rf/dispatch [::event/print-logsheet])}])
              :icon nil}
             {:id :dataset-analyzer
              :primary? true
              :title (translate [:home-card :dataset-analyzer :title]
                                "Analyse Dataset")
              :desc (translate [:home-card :dataset-analyzer :description]
                               "Preview the Overall, TWT, Burner status of latest published dataset")
              :icon nil}
             {:id :trendline
              :primary? true
              :title (translate [:home-card :trendline :title]
                                "Trendline Graph")
              :desc (translate [:home-card :trendline :description]
                               "Overview of all the recorded datasets")
              :icon nil}]
            (map #(assoc % :access (get-in access-rule [:card (:id %)])
                         :on-select on-select))
            (map (fn [props]
                   ^{:key (:id props)} [card props]))))]
     [:div (use-sub-style style/home :secondary-row)
      (doall
       (->>
        [{:id :settings
          :title (translate [:home-card :settings :title]
                            "Plant Settings")
          :desc (translate [:home-card :settings :description]
                           "Manage pyrometer, custom emissivity")}
         {:id :config-history
          :title (translate [:home-card :config-history :title]
                            "Configuration History") 
          :desc (translate [:home-card :config-history :description]
                           "")}
         {:id :goldcup
          :title (translate [:home-card :goldcup :title]
                            "GoldCup")
          :desc (translate [:home-card :goldcup :description] "")}
         {:id :config
          :title (translate [:home-card :config :title]
                            "Configure Plant")
          :desc (translate [:home-card :config :description]
                           "")}
         {:id :logs
          :title (translate [:home-card :logs :title]
                            "Logs")
          :desc (translate [:home-card :logs :description]
                           "All deleted dataset logs that can be auto recovered")}]
        (map #(assoc % :access (get-in access-rule [:card (:id %)])
                     :on-select on-select))
        (map (fn [props]
               ^{:key (:id props)} [card props]))))]]))
