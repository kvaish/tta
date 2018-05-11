(ns tta.component.root.view
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [cljs-react-material-ui.reagent :as ui]
            [cljs-react-material-ui.icons :as mic]
            [ht.util.interop :as i]
            [ht.style :refer [root-layout color-hex]]
            [ht.app.style :as ht-style]
            [ht.app.subs :as ht-subs :refer [translate]]
            [ht.app.event :as ht-event]
            [tta.util.auth :as auth]
            [tta.app.icon :as ic]
            [tta.app.comp :as app-comp]
            [tta.app.style :as app-style]
            [tta.app.subs :as app-subs]
            [tta.app.event :as app-event]
            [tta.app.view :as app-view]
            [tta.component.root.style :as style]
            [tta.component.root.subs :as subs]
            [tta.component.root.event :as event]
            [tta.dialog.user-agreement.view :refer [user-agreement]]
            [tta.dialog.choose-client.view :refer [choose-client]]
            [tta.dialog.choose-plant.view :refer [choose-plant]]
            [tta.component.home.view :refer [home]]
            [tta.component.config.view :refer [config]]
            [tta.component.settings.view :refer [settings]]
            [tta.component.dataset.view :refer [dataset]]
            [tta.app.scroll :as scroll]))

;;; language-menu ;;;

(defn language-menu [props]
  (let [anchor-el (:language @(:anchors props))
        options @(rf/subscribe [::ht-subs/language-options])
        active @(rf/subscribe [::ht-subs/active-language])]
    [app-comp/popover
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

(defn as-left-icon [icon]
  (r/as-element [:span [icon {:style {:position "absolute"}}]]))

(def settings-menu-data
  {:top [{:id :choose-plant
          :disabled? false
          :hidden? false
          :icon (as-left-icon ic/plant)
          :label-fn #(translate [:root :menu :choose-plant] "Choose plant")
          :event-id ::event/choose-plant}
         {:id :my-apps
          :icon (as-left-icon ic/my-apps)
          :label-fn #(translate [:root :menu :my-apps] "My apps")
          :event-id ::event/my-apps}]
   :bottom [{:id :logout
             :icon (as-left-icon ic/logout)
             :label-fn #(translate [:root :menu :logout] "Logout")
             :event-id ::event/logout}]
   :middle {:home tta.component.home.view/context-menu
            :dataset []
            :trendline []
            :config []
            :settings []
            :gold-cup []
            :config-history []
            :logs []}})

(defn settings-sub-menu [props]
  (let [{:keys [menu-items]} props]
    (doall
     (map (fn [{:keys [id event-id
                      disabled? hidden?
                      icon label-fn]}]
            (if-not hidden?
              [ui/menu-item
               {:key id
                :disabled disabled?
                :left-icon icon
                :primary-text (label-fn)
                :on-click #(do
                             (rf/dispatch [::event/set-menu-open? :settings false])
                             (rf/dispatch [event-id]))}]))
          menu-items))))

(defn settings-menu [props]
  (let [anchor-el (:settings @(:anchors props))
        content-id @(rf/subscribe [::subs/active-content])
        allow-content? @(rf/subscribe [::subs/content-allowed? content-id])
        context-menu (let [f (if allow-content?
                               (get-in settings-menu-data [:middle content-id]))]
                       (->> (if (fn? f) (f) f)
                            (remove nil?)
                            (not-empty)))]
    [app-comp/popover
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
            [:a (merge (use-sub-style style/header :link) {:on-click action})
             [icon {:style {:width "18px", :height "18px"}, :color "white"}]
             [:span (use-sub-style style/header :link-label) label]])
          [[:language
            mic/action-translate
            nil
            #(do
               (i/ocall % :preventDefault)
               (swap! anchors assoc :language (i/oget % :currentTarget))
               (rf/dispatch [::event/set-menu-open? :language true]))]
           [:settings
            mic/navigation-arrow-drop-right
            (translate [:header-link :settings :label] "Settings")
            #(do
               (i/ocall % :preventDefault)
               (swap! anchors assoc :settings (i/oget % :currentTarget))
               (rf/dispatch [::event/set-menu-open? :settings true]))]]))
        [language-menu {:anchors anchors}]
        [settings-menu {:anchors anchors}]]])))


;;; sub-header ;;;

(defn app-logo [props]
  [:div (assoc-in props [:style :user-select] "none")
   [:span {:style {:font-family "arial"
                   :font-weight "900"
                   :font-size "18px"}} "True"]
   [:span {:style {:font-weight "300"
                   :font-size "18px"}} "Temp"]
   [:span {:style {:font-size "8px"
                   :font-weight 700
                   :vertical-align "text-top"}} "TM"]])

(defn hot-links []
  [:div (use-style style/hot-links)
   (let [active-content @(rf/subscribe [::subs/active-content])]
     (if (= :home active-content)
       [:span]
       (doall
         (map
           (fn [[id label]]
             (let [active? (= id active-content)]
               ^{:key id}
               [:a
                (merge (use-sub-style style/hot-links
                                      (if active? :active-link :link))
                       {:href "#"
                        :on-click (if-not active?
                                    #(rf/dispatch [::event/activate-content id]))})
                label]))
           [[:home
             (translate [:quick-launch :home :label] "Home")]
            [:dataset
             (translate [:quick-launch :dataset :label] "Dataset")]
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

(defn content [{:keys [width height]}]
  (let [active-content   @(rf/subscribe [::subs/active-content])
        content-allowed? @(rf/subscribe [::subs/content-allowed?])
        content-size {:width width, :height height}]
    [:div (update (use-style style/content) :style
                  assoc :height height)
     (if content-allowed?
       (case active-content
         :home [home {:on-select #(rf/dispatch
                                   (into [::event/activate-content] %&))
                      :size content-size}]
         ;; primary
         :dataset        [dataset {:size content-size}]
         :trendline      [:div {:size content-size} "trendline"]
         ;; secondary
         :config         [config {:size content-size}]
         :settings       [settings {:size content-size}]
         :goldcup        [:div {:size content-size} "goldcup"]
         :config-history [:div {:size content-size} "config-history"]
         :logs           [:div {:size content-size} "logs"])
       ;; have no rights!!
       [no-access])]))

;;; dummy component to keep some subscriptions active
;;; these are injected to some event handlers as cofx.
;;; keeping them subscribed will eliminate the warning
;;; that they are getting disposed.
;;; OPTIMIZE: re-factor related codes so as to not require this
(defn my-subs-cache []
  @(rf/subscribe [:ht.app.subs/user-roles])
  @(rf/subscribe [:tta.component.dataset.subs/data])
  @(rf/subscribe [:tta.component.dataset.subs/settings])
  @(rf/subscribe [:tta.component.dataset.subs/config])
  @(rf/subscribe [:tta.component.dataset.subs/mode])
  @(rf/subscribe [:tta.dialog.dataset-settings.subs/active-pyrometer])
  @(rf/subscribe [:tta.dialog.dataset-settings.subs/emissivity-type])
  @(rf/subscribe [:tta.dialog.dataset-settings.subs/emissivity])
  @(rf/subscribe [:tta.dialog.dataset-settings.subs/can-submit?])
  @(rf/subscribe [:tta.dialog.dataset-settings.subs/firing])
  @(rf/subscribe [:tta.component.config.subs/data])
  @(rf/subscribe [:tta.dialog.view-factor.subs/level-opts])
  @(rf/subscribe [:tta.dialog.view-factor.subs/config])
  @(rf/subscribe [:tta.component.home.subs/draft])

  ;; dummy
  [:span])

;;; root ;;;

(defn content-box []
  (let [{:keys [ head-row-height sub-head-row-height]
         {min-h :height, min-w :width} :min-content-size} root-layout
        {:keys [width height]} @(rf/subscribe [::ht-subs/view-size])
        height (- height head-row-height sub-head-row-height)
        h (max height min-h)
        w (max width min-w)
        warning (translate [:warning :tiny-screen :message]
                           "Please use a screen with minimum resolution {resolution}!"
                           {:resolution "1024x768"})]
    (if (or (< height min-h) (< width min-w))
      ;; smaller than minimum, use a scroll or hide for too small
      (if (or (< height 200) (< width 400))
        ;; too small
        [:div {:style {:color (color-hex :red)
                       :font-size "12px"
                       :padding "5px 20px"}}
         warning]
        ;; use scroll
        [:div {:style {:position "relative"}}
         [scroll/scroll-box {:style {:width width, :height height}}
          [:div {:style {:width w, :height h, :overflow "hidden"}}
           [content {:width w, :height h}]]]
         [:div {:style {:position "absolute", :bottom 6, :left 0
                        :font-size "10px"
                        :color (color-hex :royal-blue)
                        :padding "0 10px"}}
          (str "* " warning)]])
      [content {:width w, :height h}])))

(defn root []
  (let [agreed? @(rf/subscribe [::subs/agreed?])
        client @(rf/subscribe [::app-subs/client])
        plant @(rf/subscribe [::app-subs/plant])
        app-allowed? @(rf/subscribe [::subs/app-allowed?])]
    [:div (use-style style/root)
     [header]
     (if app-allowed?
       (if (and agreed? client plant)
         (list ^{:key :sub-header} [sub-header]
               ^{:key :content} [content-box]))
       [no-access])

     ;;dialogs
     (if @(rf/subscribe [:tta.dialog.user-agreement.subs/open?])
       [user-agreement])
     (if @(rf/subscribe [:tta.dialog.choose-client.subs/open?])
       [choose-client])
     (if @(rf/subscribe [:tta.dialog.choose-plant.subs/open?])
       [choose-plant])

     ;; dummy subscription cache to improve performance
     [my-subs-cache]]))
