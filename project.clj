(defproject tta "1.0.0.D.1"
  :dependencies [[org.clojure/clojure "1.9.0"]
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
                 [cljsjs/react "16.1.1-0"]
                 [cljsjs/react-dom "16.1.1-0"]
                 [cljsjs/react-dom-server "16.1.1-0"]
                 [garden "1.3.3"]
                 [stylefy "1.1.0"
                  :exclusions [garden]]
                 [cljs-http "0.1.44"
                  :exclusions [com.cognitect/transit-cljs]]
                 [com.andrewmcveigh/cljs-time "0.5.2"]
                 [cljsjs/d3 "4.12.0-0"]
                 [cljsjs/filesaverjs "1.3.3-0"]
                 [com.cognitect/transit-cljs "0.8.243"]
                 [figwheel-sidecar "0.5.14"
                  :exclusions [org.clojure/tools.nrepl]]]

  :plugins [[lein-figwheel "0.5.14"
             :exclusions [org.clojure/clojure]]
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

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :aliases {"build" ["with-profile" "prd,user"
                     ["do"
                      ["clean"]
                      ["run" "-m" "ht.exports"]
                      ["garden" "once"]
                      ["cljsbuild" "once" "min"]]]}

  :source-paths ["src/clj" "src/cljc"]

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.8"]
                   [re-frisk "0.5.2"
                    :exclusions [org.clojure/clojure]]
                   [com.cemerick/piggieback "0.2.2"]
                   [org.clojure/data.json "0.2.6"
                    :exclusions [org.clojure/clojure]]]

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
                    :on-jsload "tta.core/mount-root"}
     :compiler     {:main                 tta.core
                    :output-to            "resources/public/js/dev/app.js"
                    :output-dir           "resources/public/js/dev/out"
                    :asset-path           "js/dev/out"
                    :source-map-timestamp true
                    :preloads             [devtools.preload
                                           re-frisk.preload]
                    :external-config      {:devtools/config
                                           {:features-to-install :all}}}}

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
