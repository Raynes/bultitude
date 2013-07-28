(ns bultitude.core-test
  (:use clojure.test
        bultitude.core
        [clojure.pprint :only [pprint]])
  (require [clojure.java.io :as io])
  (:import (java.io File BufferedReader PushbackReader InputStreamReader)
           (java.util.jar JarFile JarEntry)))

;;; Top-level tests

(deftest namespaces-on-classpath-test
  (testing "find clojure.core"
    (is (seq (filter
              #(= 'clojure.core %)
              (namespaces-on-classpath)))))
  (testing "prefix"
    (is (seq (filter
              #(= 'clojure.core %)
              (namespaces-on-classpath :prefix "clojure.core"))))
    (is (every?
         #(.startsWith (name %) "clojure.core")
         (namespaces-on-classpath :prefix "clojure.core"))))
  (testing "directory"
    (is (=
         #{'bultitude.core 'bultitude.core-test}
         (set (namespaces-on-classpath :prefix "bultitude")))))
  (testing "dash handling in prefixes"
    (is (=
         #{'bulti-tude.test}
         (set (namespaces-on-classpath :prefix "bulti-tude"))))))

(deftest namespaces-in-dir-test
  (is (= '#{bulti-tude.test bultitude.core-test}
         (set (namespaces-in-dir "test")))))

;; Don't know why `namespaces-in-jar` is private while `namespaces-in-dir` isn't.
;; Since it is, it's tested below, in "utilities".


;; The above functions are the main ones, but the following 
;; are useful for tools that want to work with the namespace
;; classifications rather than just namespaces.

(deftest select-subdirectory-test
  (is (= (io/file "./test/bulti_tude")
         (select-subdirectory (io/file ".") "test.bulti-tude")))

  (is (= (io/file "test")
         (select-subdirectory (io/file "test") nil)))

  (is (= (io/file "test")
         (select-subdirectory (io/file "test") ""))))

(deftest filter-by-prefix-test
  (is (= '[bultitude.core]
         (filter-by-prefix '[bultitude.core clojure.test] "bultitude")))
  (is (= '[bultitude.core clojure.test]
         (filter-by-prefix '[bultitude.core clojure.test] nil))))

(defn formatted-actual [result-maps]
  (set (map #(assoc % :file (.getName (:file %))) result-maps)))

(defn selected-actual [formatted-actual]
  (set (map #(select-keys % [:namespace-symbol :status :file])
            formatted-actual)))

(deftest classify-dir-entries-test
  (let [result (formatted-actual  (classify-dir-entries "test"))
        expected #{ {:status :contains-namespace
                     :file "test.clj"
                     :namespace-symbol 'bulti-tude.test}
                    
                    {:status :no-attempt-at-namespace
                     :file "clojure-file-without-a-namespace.clj"}
                    
                    {:status :contains-namespace
                     :file "core_test.clj"
                     :namespace-symbol 'bultitude.core-test}
                    
                    {:status :invalid-clojure-file
                     :file "invalid.clj"}}]

    (is (= expected (selected-actual result)))
    (is (= #{:standalone-file}  (set (map :source-type result))))))

(deftest classify-jar-entries-test
  (let [result (formatted-actual (classify-jar-entries (io/file "test/test.jar")))
        expected #{ {:status :contains-namespace
                     :file "bulti_tude/test.clj"
                     :namespace-symbol 'bulti-tude.test}
                    
                    {:status :no-attempt-at-namespace
                     :file "bultitude/clojure-file-without-a-namespace.clj"}
                    
                    {:status :contains-namespace
                     :file "bultitude/core_test.clj"
                     :namespace-symbol 'bultitude.core-test}
                    
                    {:status :invalid-clojure-file
                     :file "bultitude/invalid.clj"}}]
    (is (= expected (selected-actual result)))
    (is (= #{:jar-entry}  (set (map :source-type result))))
    (is (= #{"test/test.jar"} (set (map #(.getName (:jarfile %)) result))))))


;;; Utilities

(deftest describe-namespace-status-test
  (let [subject #'bultitude.core/describe-namespace-status
        as-reader #(PushbackReader. (java.io.StringReader. %))]
    ;; success cases
    (is (= {:status :contains-namespace
            :namespace-symbol 'foo}
         (subject (as-reader "(ns foo)"))))
    (is (= {:status :contains-namespace
            :namespace-symbol 'foo}
         (subject (as-reader "1 (ns foo)"))))
    ;; Note: it doesn't matter if the file is broken
    ;; after the namespace is recognized
    (is (= {:status :contains-namespace
            :namespace-symbol 'foo}
         (subject (as-reader "1 (ns foo) ("))))

    ;; No attempt at namespaces
    (is (= {:status :no-attempt-at-namespace}
           (subject (as-reader ""))))
    (is (= {:status :no-attempt-at-namespace}
           (subject (as-reader "1"))))
    (is (= {:status :no-attempt-at-namespace}
           (subject (as-reader "(defn fact [n] (inc n))"))))

    ;; Broken Clojure files
    (is (= {:status :invalid-clojure-file}
           (subject (as-reader "(ns foo"))))
    (is (= {:status :invalid-clojure-file}
           (subject (as-reader "(ns foo]"))))))
    

(deftest namespaces-in-jar-test
  (is (= (set (#'bultitude.core/namespaces-in-jar (io/file "test/test.jar")))
         (set '[bulti-tude.test bultitude.core-test]))))
         
