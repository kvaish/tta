(ns tta.component.root.view
  (:require [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [tta.util.common :refer [translate]]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.app.view :as app-view]
            [tta.component.root.style :as style]
            [tta.component.root.subs :as subs]
            [tta.component.root.event :as event]
            [tta.component.home.view :refer [home]]))

;;; header ;;;

(defn header []
  [:div (use-style style/header)
   [:div (use-sub-style style/header :logo)]
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
        #(rf/dispatch [::event/show-settings-menu])]
       [:logout
        nil
        (translate [:header-link :logout :label] "Logout")
        #(rf/dispatch [::app-event/logout])]]))]])


;;; sub-header ;;;

(defn sub-header-left []
  [:div (use-style style/sub-header-left)
   [:span {:style {:font-family "arial"
                   :font-weight "900"
                   :font-size "18px"}} "True"]
   [:span {:style {:font-weight "300"
                   :font-size "18px"}} "Temp™"]])

(defn sub-header-middle []
  (let [active-content @(rf/subscribe [::subs/active-content])]
    (if (= :home active-content)
      [:span]
      [:div (use-style style/sub-header-middle)
       (doall
        (map
         (fn [[id label target]]
           (let [target (or target id)
                 active? (= target active-content)]
             ^{:key id}
             [:a
              (merge (use-sub-style style/sub-header-middle
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
           (translate [:quickLaunch :trendline :label] "Trendline")]]))])))

(defn info [label text]
  [:div (use-style style/sub-header-right)
   [:p (use-sub-style style/sub-header-right :info-p)
    [:span (use-sub-style style/sub-header-right :info-head)
     label]
    [:br]
    [:span (use-sub-style style/sub-header-right :info-body)
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
   [sub-header-left]
   [sub-header-middle]
   [plant-info]
   [company-info]])


;;; content ;;;

(defn no-access []
  [:div (use-style style/no-access)
   [:p (use-sub-style style/no-access :p)
    (translate [:root :noAccess :message] "Insufficient rights!")]])

(defn content []
  (let [view-size @(rf/subscribe [::app-subs/view-size])
        active-content @(rf/subscribe [::subs/active-content])
        content-allowed? @(rf/subscribe [::subs/content-allowed?])]
    [:div (update (use-style style/content) :style
                  assoc :height (style/content-height view-size))
     (if content-allowed?
       (case active-content
         :home [home {:on-select #(rf/dispatch [::event/activate-content %])}]
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
  [:div (use-style style/root)
   [header]
   [sub-header]
   [content]
   [app-view/busy-screen]])


