(defproject tta "1.0.0.D.17"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"] ;;  "1.9.64"
                 [org.clojure/core.async "0.4.474"] ;; "0.3.465"
                 [reagent "0.8.0" ;; "0.7.0"
                  :exclusions [cljsjs/react
                               cljsjs/react-dom
                               cljsjs/react-dom-server]]
                 [reagent-utils "0.3.1"]
                 [re-frame "0.10.5"] ;; "0.10.2"
                 [re-frame-utils "0.1.0"]
                 [day8.re-frame/forward-events-fx "0.0.5"]
                 #_[day8.re-frame/tracing-stubs "0.5.1"]
                 [cljsjs/material-ui "0.19.2-0"]
                 [cljs-react-material-ui "0.2.50"
                  :exclusions [org.clojure/clojure
                               cljsjs/material-ui]]
                 [cljsjs/react "16.3.2-0"] ;; "16.2.0-3"
                 [cljsjs/react-dom "16.3.2-0"] ;; "16.2.0-3"
                 [cljsjs/react-dom-server "16.3.2-0"] ;; "16.2.0-3"
                 [garden "1.3.5"] ;;  "1.3.4"
                 [stylefy "1.4.2" ;; "1.2.0"
                  :exclusions [garden]]
                 [cljs-http "0.1.45" ;; "0.1.44"
                  :exclusions [com.cognitect/transit-cljs]]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [cljsjs/d3 "4.12.0-0"]
                 [cljsjs/react-motion "0.5.0-0"]
                 [cljsjs/filesaverjs "1.3.3-0"]
                 [com.cognitect/transit-cljs "0.8.256"] ;; "0.8.243"
                 [figwheel-sidecar "0.5.15" ;; "0.5.14"
                  :exclusions [org.clojure/tools.nrepl]]]

  :plugins [[lein-figwheel "0.5.15" ;; "0.5.14"
             :exclusions [org.clojure/clojure]]
            ;; [lein-re-frisk "0.5.8"]
            [lein-cljsbuild "1.1.7"]
            [lein-garden "0.3.0"
             :exclusions [garden org.clojure/clojure]]
            [lein-doo "0.1.8"]]

  :min-lein-version "2.8.1"

  :clean-targets ^{:protect false} ["resources/public/js/dev"
                                    "resources/public/js/min"
                                    "resources/public/css/build"
                                    "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 3450}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]
                 :timeout 120000}

  :aliases {"build" ["with-profile" "prd,user"
                     ["do"
                      ["clean"]
                      ["run" "-m" "ht.exports"]
                      ["garden" "once"]
                      ["cljsbuild" "once" "min"]]]
            "start" ["do"
                     ["clean"]
                     ["garden" "once"]
                     ["repl"]]}

  :source-paths ["src/clj" "src/cljc"]

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"] ;; "0.9.9"
                   [re-frisk "0.5.4" ;; "0.5.3"
                    :exclusions [org.clojure/clojure]]
                   #_[re-frisk-remote "0.5.5"
                    :exclusions [org.clojure/clojure]]
                   [com.cemerick/piggieback "0.2.2"]
                   [org.clojure/data.json "0.2.6"
                    :exclusions [org.clojure/clojure]]
                   #_[day8.re-frame/tracing "0.5.1"]
                   #_[day8.re-frame/re-frame-10x "0.3.3-react16"]]

    :garden
    {:builds
     [{:id "style" ;; optional name of the build
       :source-paths ["src/clj" "src/cljc"]
       :stylesheet tta.style/app-styles
       :compiler {:output-to "resources/public/css/build/style.css"
                  :pretty-print? true}}]}}

   :prd
   {:garden
    {:builds
     [{:id "style" ;; optional name of the build
       :source-paths ["src/clj" "src/cljc"]
       :stylesheet tta.style/app-styles
       :compiler {:output-to "resources/public/css/build/style.css"
                  :pretty-print? false}}]}}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs" "src/cljc" "src/dev-cljs"]
     :figwheel     {:websocket-host :js-client-host
                    :on-jsload "dev.load/on-jsload"}
     :compiler     {:main                 dev.load
                    :output-to            "resources/public/js/dev/app.js"
                    :output-dir           "resources/public/js/dev/out"
                    :asset-path           "js/dev/out"
                    :source-map-timestamp true
                    :closure-defines
                    {"day8.re_frame.tracing.trace_enabled_QMARK_" true
                     "re_frame.trace.trace_enabled_QMARK_" true}
                    :preloads             [devtools.preload
                                           re-frisk.preload
                                           #_day8.re-frame-10x.preload]
                    :external-config      {:devtools/config
                                           {:features-to-install
                                            [:formatters :hints]}}}}

    {:id           "min"
     :source-paths ["src/cljs" "src/cljc" "src/prd-cljs"]
     :compiler     {:main            tta.core
                    :output-to       "resources/public/js/min/app.js"
                    :language-in     :ecmascript5
                    :language-out    :ecmascript5
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false
                    :externs ["externs.js"]}}]})
