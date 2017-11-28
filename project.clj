(defproject tta "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0-RC1"]
                 [org.clojure/clojurescript "1.9.946"]
                 [org.clojure/core.async "0.3.465"]
                 [reagent "0.7.0"
                  :exclusions [cljsjs/react
                               cljsjs/react-dom
                               cljsjs/react-dom-server]]
                 [re-frame "0.10.2"]
                 [cljsjs/material-ui "0.19.2-0"]
                 [cljs-react-material-ui "0.2.50"
                  :exclusions [org.clojure/clojure
                               cljsjs/material-ui]]
                 [cljsjs/react "16.1.0-1"]
                 [cljsjs/react-dom "16.1.0-1"]
                 [cljsjs/react-dom-server "16.1.0-1"]
                 [garden "1.3.3"]
                 [stylefy "1.1.0"
                  :exclusions [garden]]
                 [cljs-http "0.1.44"
                  :exclusions [com.cognitect/transit-cljs]]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [cljsjs/d3 "4.12.0-0"]
                 [com.cognitect/transit-cljs "0.8.243"]]

  :min-lein-version "2.8.1"

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/dev"
                                    "resources/public/js/min"
                                    "target"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :aliases {"build" ["do" ["clean"] ["cljsbuild" "once" "min"]]}
  
  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.7"]
                   [re-frisk "0.5.2"
                    :exclusions [org.clojure/clojure]]
                   [figwheel-sidecar "0.5.14"
                    :exclusions [org.clojure/tools.nrepl]]
                   [com.cemerick/piggieback "0.2.2"]]
    :plugins      [[lein-figwheel "0.5.14"
                    :exclusions [org.clojure/clojure]]
                   [lein-cljsbuild "1.1.7"]]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs" "src/dev-cljs"]
     :figwheel     {:websocket-host :js-client-host
                    :on-jsload "tta.core/mount-root"}
     :compiler     {:main                 tta.core
                    :output-to            "resources/public/js/dev/app.js"
                    :output-dir           "resources/public/js/dev/out"
                    :asset-path           "js/dev/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           re-frisk.preload]
                    :external-config      {:devtools/config
                                           {:features-to-install :all}}
                    }}

    {:id           "min"
     :source-paths ["src/cljs" "src/prd-cljs"]
     :compiler     {:main            tta.core
                    :output-to       "resources/public/js/min/app.js"
                    :language-in     :ecmascript5
                    :language-out    :ecmascript5
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false
                    :externs ["externs.js"]}}


    ]})
