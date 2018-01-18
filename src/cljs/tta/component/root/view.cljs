(ns tta.component.root.view
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
            [tta.app.view :as app-view]
            [tta.component.root.style :as style]
            [tta.component.root.subs :as subs]
            [tta.component.root.event :as event]
            [tta.component.home.view :refer [home]]
            [tta.dialog.user-agreement.view :refer [user-agreement]]
            [tta.dialog.user-agreement.event :as ua-event]
            [tta.dialog.user-agreement.subs :as ua-subs]
            [tta.dialog.choose-client.subs :as cc-subs]
            [tta.dialog.choose-client.event :as cc-event]
            [tta.dialog.choose-client.view :refer [choose-client]]))

;;; header ;;;

(defn header []
  [:div (use-style style/header)
   [:div (use-sub-style style/header :left)]
   [:div (use-sub-style style/header :middle)]
   [:div (use-sub-style style/header :right)
    (doall
     (map
      (fn [[id icon label action]]
        ^{:key id}
        [:a (merge (use-sub-style style/header :link)
                   {:href "javascript:void(0);" :on-click action })
         (if icon
           [:i (use-sub-style style/header
                              (if (= id :language) :icon-only :link-icon)
                              {::stylefy/with-classes [icon]})])
         label])
      [[:language
        "fa fa-language"
        nil
        #(rf/dispatch [::event/show-language-menu])]
       [:settings
        "fa fa-caret-right"
        (translate [:header-link :settings :label] "Settings")
        #(rf/dispatch [::event/show-settings-menu])]]))]])


;;; sub-header ;;;

(defn app-logo [props]
    [:div props
     [:span {:style {:font-family "arial"
                     :font-weight "900"
                     :font-size "18px"}} "True"]
     [:span {:style {:font-weight "300"
                     :font-size "18px"}} "Temp"]
     [:span {:style {:font-size "12px"
                     :vertical-align "text-top"}} "™"]])

(defn hot-links []
  [:div (use-style style/hot-links)
   (let [active-content @(rf/subscribe [::subs/active-content])]
     (if (= :home active-content)
       [:span]
       (doall
        (map
         (fn [[id label target]]
           (let [target (or target id)
                 active? (= target active-content)]
             ^{:key id}
             [:a
              (merge (use-sub-style style/hot-links
                                    (if active? :active-link :link))
                     {:href "#"
                      :on-click (if-not active?
                                  #(rf/dispatch [::event/activate-content target]))})
              label]))
         [[:home
           (translate [:quickLaunch :home :label] "Home")]
          [:dataset
           (translate [:quickLaunch :dataset :label] "Dataset")
           @(rf/subscribe [::subs/active-dataset-action])]
          [:trendline
           (translate [:quickLaunch :trendline :label] "Trendline")]]))))])

(defn messages [] ;; TODO: define comp for message and warning
  [:div (use-style style/messages)])

(defn info [label text]
  [:div (use-style style/info)
   [:p (use-sub-style style/info :p)
    [:span (use-sub-style style/info :head)
     label]
    [:span (use-sub-style style/info :body)
     text]]])

(defn company-info []
  (let [client @(rf/subscribe [::app-subs/active-client])
        label (translate [:info :company :label] "Company")]
    (info label (:name client))))

(defn plant-info []
  (let [plant @(rf/subscribe [::app-subs/active-plant])
        label (translate [:info :plant :label] "Plant")]
    (info label (:name plant))))

(defn sub-header []
  [:div (use-style style/sub-header)
   [:div (use-sub-style style/sub-header :left)
    [app-logo (use-sub-style style/sub-header :logo)]
    [hot-links]
    [:div (use-sub-style style/sub-header :spacer)]
    [messages]]
   [:div (use-sub-style style/sub-header :right)
    [company-info]
    [plant-info]]])

;;; content ;;;

(defn no-access []
  [:div (use-style style/no-access)
   [:p (use-sub-style style/no-access :p)
    (translate [:root :noAccess :message] "Insufficient rights!")]])

(defn disclaimer-reject []
  [:div 
   (use-style style/disclaimer-reject)
     [:p (use-sub-style style/disclaimer-reject :p)
      (translate [:root :disclaimerReject :message]
                 "Use of this application is prohibited without agreement!")]
   [:div (use-style style/close-button)
    [:i {:class "fa fa-times"}]
    ]
   [:div  (use-style style/disclaimer-reject-buttons) 
    [ui/raised-button
     {:label (translate [:app :disclaimer :retry] "Retry")
      :on-click #(rf/dispatch [::ua-event/open {}])}]
    [ui/raised-button
     {:label (translate [:app :disclaimer :exit] "Exit")
      }]]])

(defn content []
  (let [view-size @(rf/subscribe [::ht-subs/view-size])
        active-content @(rf/subscribe [::subs/active-content])
        content-allowed? @(rf/subscribe [::subs/content-allowed?])]
    [:div (update (use-style style/content) :style
                  assoc :height (style/content-height view-size))
     (if content-allowed?
       (case active-content
         :home [home {:on-select #(rf/dispatch [::event/activate-content %])}
                :on-load #(rf/dispatch [::ht-event/fetch-auth] )]
         ;; :home  [:div "home"]
         ;; primary
         :dataset-creator   [:div "dataset-creator"]
         :dataset-analyzer  [:div "dataset-analyzer"]
         :trendline         [:div "trendline"]
         ;; secondary
         :settings          [:div "settings"]
         :config-history    [:div "config-history"]
         :goldcup           [:div "goldcup"]
         :config            [:div "config"]
         :logs              [:div "logs"])
       ;; have no rights!!
       [no-access])])) 

;;; root ;;;

(defn root []
  (let [active-user @(rf/subscribe [::app-subs/active-user])
        is-agreed @(rf/subscribe [::subs/agreed?])]
    [:div (use-style style/root)
     [header]
     (if is-agreed
       (list
        [sub-header]
        [content]))
     (if-not is-agreed
       [user-agreement])
     (if (false? is-agreed)
       [disclaimer-reject])]))