(defproject timofreiberg/bultitude "0.2.9"
  :description "A library for finding Clojure namespaces on the classpath."
  :url "https://github.com/timofreiberg/bultitude"
  :license {:name "Eclipse Public License 1.0"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.tcrawley/dynapath "1.0.0"]]
  :aliases {"test-all" ["with-profile" "dev,default:dev,1.8:dev,1.7:dev,1.6:dev,1.5:dev,1.4:dev,1.3,dev" "test"]}
  :profiles {:test {:resources ["test-resources"]}
             :1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :1.7 {:dependencies [[org.clojure/clojure "1.7.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :1.5 {:dependencies [[org.clojure/clojure "1.5.1"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}})
