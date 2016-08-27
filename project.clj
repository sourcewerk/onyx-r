(defproject sourcewerk/onyx-r "0.1.0-SNAPSHOT"
  :description "Onyx Task Bundle for Implementing Data Processing Tasks in R"
  :url "https://github.com/sourcewerk/onyx-r"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[aero "1.0.0-beta2"]
                 [org.clojure/clojure "1.8.0"]
                 [org.onyxplatform/onyx "0.9.9"]
                 ;[org.onyxplatform/lib-onyx "0.9.0.1"] ; TODO
                 [org.rosuda.REngine/Rserve "1.8.1"]]
  :source-paths ["src"]
  :profiles {:dev {:jvm-opts ["-XX:-OmitStackTraceInFastThrow"]
                   :global-vars {*assert* true}}
             :dependencies [[org.clojure/tools.namespace "0.2.11"]
                            [lein-project-version "0.1.0"]]})
