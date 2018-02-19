(ns ht.app.view
  "collection of common small view elements for re-use"
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [cljs-react-material-ui.core :refer [get-mui-theme]]
            [cljs-react-material-ui.reagent :as ui]
            [stylefy.core :as stylefy :refer [use-style use-sub-style]]
            [ht.work.view] ;; dev purpose only!
            [ht.app.style :as style]
            [ht.app.subs :as subs :refer [translate]]
            [ht.app.event :as event]))

(defn busy-screen []
  (let [busy? @(rf/subscribe [::subs/busy?])
        {:keys [content paper spinner]} style/busy-screen]
    (if busy?
      [ui/dialog
       {:open true
        :modal true
        :contentStyle content
        :paperProps {:style paper}}
       [:div (use-style spinner)]])))

(defn app-fault [{:keys [icon-class header message label fatal?]}]
  (let [{:keys [title icon]} style/app-fault
        exit #(rf/dispatch [::event/exit])
        close-label (translate [:action :close :label] "Close")
        close #(rf/dispatch [::event/service-failure false nil])]
    [ui/dialog
     {:open true, :modal true
      :title (r/as-element
              [:p {:style title}
               [ui/font-icon {:style icon
                              :class-name icon-class}]
               header])
      :actions [(r/as-element [ui/flat-button {:label label
                                               :on-click exit}])
                (if-not fatal?
                  (r/as-element [ui/flat-button {:label close-label
                                                 :on-click close}]))]}
     message]))

(defn no-claims []
  (app-fault {:fatal? true, :icon-class "fa fa-lock"
              :header (translate [:root :failure :auth] "Unauthorized!")
              :label (translate [:action :login :label] "Login")}))

(defn service-failure []
  (let [{:keys [fatal? status error message]} @(rf/subscribe [::subs/service-failure])]
    (if (some? status)
      (app-fault {:fatal? fatal?, :icon-class "fa fa-exclamation-circle"
                  :header (translate [:root :failure :service] "Service unavailable!")
                  :label (translate [:action :portal :label] "Portal")
                  :message (str status " - " error " - " message)}))))

(defn message-box []
  (let [{:keys [open? message title level
                label-ok event-ok
                label-cancel event-cancel]} @(rf/subscribe [::subs/message-box])]
    (if open?
      [ui/dialog
       {:open open?
        :modal true
        :title (case level
                 (:error :warning)
                 (r/as-element
                  [:p
                   [ui/font-icon {:style (:icon (style/message-box level))
                                  :class-name (case level
                                                :error "fa fa-exclamation-circle"
                                                :warning "fa fa-exclamation-triangle")}]
                   title])
                 ;; default
                 title)
        :actions [(r/as-element
                   [ui/flat-button
                    {:label (or label-ok
                                (translate [:root :message-box :ok] "Ok"))
                     :on-click #(do
                                  (rf/dispatch [::event/close-message-box])
                                  (rf/dispatch event-ok))}])
                  (if label-cancel
                    (r/as-element
                     [ui/flat-button
                      {:label label-cancel
                       :on-click #(do
                                    (rf/dispatch [::event/close-message-box])
                                    (if event-cancel
                                      (rf/dispatch event-cancel)))}]))]}
       message])))

(defn create-app [root]
  (fn []
    [ui/mui-theme-provider
     {:mui-theme (get-mui-theme style/theme)}
     [:div
      (let [claims @(rf/subscribe [::subs/auth-claims])]
        (cond
          claims        [root]
          (false? claims) [no-claims]))
      [ht.work.view/work] ;; has no effect in prod build. only for dev!
      [message-box]
      [busy-screen]
      [service-failure]]]))
