(defproject bultitude "0.2.8"
  :min-lein-version "2.0.0"
  :description "A library for find Clojure namespaces on the classpath."
  :url "https://github.com/Raynes/bultitude"
  :license {:name "Eclipse Public License 1.0"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.tcrawley/dynapath "0.2.3"]]
  :aliases {"test-all" ["with-profile" "dev,default:dev,1.6:dev,1.5:dev,1.4:dev,1.3,dev" "test"]}
  :profiles {:test {:resources ["test-resources"]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}})
