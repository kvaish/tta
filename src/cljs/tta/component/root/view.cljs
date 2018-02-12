(ns tta.component.root.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [ht.util.interop :as i]
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
            [tta.dialog.choose-client.view :refer [choose-client]]
            [tta.dialog.choose-plant.view :refer [choose-plant]]
            [tta.util.auth :as auth]
            [tta.component.settings.view :refer [settings]]
            [tta.component.settings.event :as setting-event]
            [tta.dialog.edit-pyrometer.view :refer [edit-pyrometer]]
            [tta.dialog.custom-emissivity.view :refer [custom-emissivity]]))

;;; language-menu ;;;

(defn language-menu [props]
  (let [anchor-el (:language @(:anchors props))
        options @(rf/subscribe [::ht-subs/language-options])
        active @(rf/subscribe [::ht-subs/active-language])]
    [ui/popover
     {:open @(rf/subscribe [::subs/menu-open? :language])
      ;; this is a workaround to hide the initial flashing
      ;; :style {:position "fixed", :top 100000}
      :anchor-el anchor-el
      :anchor-origin {:horizontal "right"
                      :vertical "bottom"}
      :target-origin {:horizontal "right"
                      :vertical "top"}
      :on-request-close #(rf/dispatch [::event/set-menu-open? :language false])}
     (into [ui/menu {:value active}]
           (map (fn [{:keys [id name]}]
                  [ui/menu-item
                   {:primary-text name
                    :on-click #(do
                                 (rf/dispatch [::event/set-menu-open? :language false])
                                 (rf/dispatch [::ht-event/set-language id]))
                    :value id}])
                options))]))

;;; settings-menu

(defn fa-icon [class]
  (r/as-element
   [ui/font-icon {:class-name class}]))

(defn svg-icon [src]
  (r/as-element
   [:img {:src src}]))

(def settings-menu-data
  {:top [{:id :choose-plant
          :disabled? false
          :hidden? false
          :icon (fa-icon "fa fa-industry")
          :label "Choose plant"
          :label-key :choose-plant
          :event-id :tta.dialog.choose-plant.event/open}
         {:id :my-apps
          :icon (fa-icon "fa fa-star")
          :label "My apps"
          :label-key :my-apps
          :event-id ::ht-event/exit}]
   :bottom [{:id :logout
             :icon (fa-icon "fa fa-sign-out")
             :label "Logout"
             :label-key :logout
             :event-id ::ht-event/logout}]
   :middle {:home tta.component.home.view/context-menu
            :dataset-creator []
            :dataset-analyzer []
            :trendline []
            :config []
            :settings []
            :goldcup []
            :config-history []
            :logs []}})

(defn settings-sub-menu [props]
  (let [{:keys [menu-items]} props]
    (doall
     (map (fn [{:keys [id event-id
                      disabled? hidden?
                      icon label label-key]}]
            (if-not hidden?
              [ui/menu-item
               {:key id
                :disabled disabled?
                :left-icon icon
                :primary-text (translate [:root :menu label-key] label)
                :on-click #(do
                             (rf/dispatch [::event/set-menu-open? :settings false])
                             (rf/dispatch [event-id]))}]))
          menu-items))))

(defn settings-menu [props]
  (let [anchor-el (:settings @(:anchors props))
        content-id @(rf/subscribe [::subs/active-content])
        allow-content? @(rf/subscribe [::subs/content-allowed? content-id])
        context-menu (not-empty (if allow-content?
                                  (get-in settings-menu-data [:middle content-id])))]
    [ui/popover
     {:open @(rf/subscribe [::subs/menu-open? :settings])
      :desktop true
      ;; this is a workaround to hide the inital flashing
      ;; :style {:position "fixed" :top 10000}
      :anchor-el anchor-el
      :anchor-origin {:horizontal "right"
                               :vertical "bottom"}
      :target-origin {:horizontal "right"
                               :vertical "top"}
      :on-request-close #(rf/dispatch [::event/set-menu-open? :settings false])}
     [ui/menu
      ;; top section
      (settings-sub-menu {:menu-items (:top settings-menu-data)})

      ;; middle (context) section
      (if context-menu
        (list
         [ui/divider {:key :div-middle}]
         (settings-sub-menu {:key :middle-sub-menu
                             :menu-items context-menu})))

      ;; bottom section
      [ui/divider]
      (settings-sub-menu {:menu-items (:bottom settings-menu-data)})]]))

;;; header ;;;

(defn header []
  (let [anchors (atom {})]
    (fn []
      [:div (use-style style/header)
       [:div (use-sub-style style/header :left)]
       [:div (use-sub-style style/header :middle)]
       [:div (use-sub-style style/header :right)
        (doall
         (map
          (fn [[id icon label action]]
            ^{:key id}
            [:a (merge (use-sub-style style/header :link)
                       {:href "#" :on-click action})
             (if icon
               [:i (use-sub-style style/header
                                  (if (= id :language) :icon-only :link-icon)
                                  {::stylefy/with-classes [icon]})])
             label])
          [[:language
            "fa fa-language"
            nil
            #(do
               (i/ocall % :preventDefault)
               (swap! anchors assoc :language (i/oget % :currentTarget))
               (rf/dispatch [::event/set-menu-open? :language true]))]
           [:settings
            "fa fa-caret-right"
            (translate [:header-link :settings :label] "Settings")
            #(do
               (i/ocall % :preventDefault)

               (swap! anchors assoc :settings (i/oget % :currentTarget))
               (rf/dispatch [::event/set-menu-open? :settings true]))]]))
        [language-menu {:anchors anchors}]
        [settings-menu {:anchors anchors}]]])))


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
           (translate [:quick-launch :home :label] "Home")]
          [:dataset
           (translate [:quick-launch :dataset :label] "Dataset")
           @(rf/subscribe [::subs/active-dataset-action])]
          [:trendline
           (translate [:quick-launch :trendline :label] "Trendline")]]))))])

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
  (let [client @(rf/subscribe [::app-subs/client])
        label (translate [:info :company :label] "Company")]
    (info label (:name client))))

(defn plant-info []
  (let [plant @(rf/subscribe [::app-subs/plant])
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
    (translate [:root :no-access :message] "Insufficient rights!")]])

(defn content []
  (let [view-size @(rf/subscribe [::ht-subs/view-size])
        active-content @(rf/subscribe [::subs/active-content])
        content-allowed? @(rf/subscribe [::subs/content-allowed?])]
    [:div (update (use-style style/content) :style
                  assoc :height (style/content-height view-size))
     (if content-allowed?
       (case active-content
         :home [home {:on-select #(rf/dispatch [::event/activate-content %])}]
         ;; primary
         :dataset-creator   [:div "dataset-creator"]
         :dataset-analyzer  [:div "dataset-analyzer"]
         :trendline         [:div "trendline"]
         ;; secondary
         :config            [:div "config"]
         :settings          [settings]
         :goldcup           [:div "goldcup"]
         :config-history    [:div "config-history"]
         :logs              [:div "logs"])
       ;; have no rights!!
       [no-access])]))

;;; root ;;;

(defn root []
  (let [agreed? @(rf/subscribe [::subs/agreed?])
        client @(rf/subscribe [::app-subs/client])
        plant @(rf/subscribe [::app-subs/plant])
        app-allowed? @(rf/subscribe [::subs/app-allowed?])]
    [:div (use-style style/root)
     [header]
     (if app-allowed?
       (if (and agreed? client plant)
         (list ^{:key :sub-header} [sub-header {:key "sub-header"}]
               ^{:key :content} [content {:key "content"}]))
       [no-access])

     ;;dialogs
     (if @(rf/subscribe [:tta.dialog.user-agreement.subs/open?])
       [user-agreement])
     (if @(rf/subscribe [:tta.dialog.choose-client.subs/open?])
       [choose-client])
     (if @(rf/subscribe [:tta.dialog.choose-plant.subs/open?])
       [choose-plant])
     (if @(rf/subscribe [:tta.dialog.edit-pyrometer.subs/open?])
       [edit-pyrometer])
     (if @(rf/subscribe [:tta.dialog.custom-emissivity.subs/open?])
       [custom-emissivity])]))
