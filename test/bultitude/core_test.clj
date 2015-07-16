(ns bultitude.core-test
  (:require [clojure.java.io :as io])
  (:use clojure.test
        bultitude.core))

(deftest namespaces-in-dir-test
  (testing namespaces-in-dir
    (is (= (if *read-cond*
             '#{bulti-tude.cond bulti-tude.test}
             '#{bulti-tude.test})
           (set (namespaces-in-dir "test/bulti_tude"))))))

(deftest namespaces-forms-in-dir-test
  (testing namespace-forms-in-dir
    (is (= (if *read-cond*
             '#{(ns bulti-tude.cond) (ns bulti-tude.test)}
             '#{(ns bulti-tude.test)})
           (set (namespace-forms-in-dir "test/bulti_tude"))))))

(deftest file->namespaces-test
  (testing "on a directory with a clj in it"
    (is (= (if *read-cond*
             '#{bulti-tude.cond bulti-tude.test}
             '#{bulti-tude.test})
           (set (file->namespaces nil (io/file "test/bulti_tude")))))))

(deftest file->namespace-forms-test
  (testing "on a directory with a clj in it"
    (is (= (if *read-cond*
             '#{(ns bulti-tude.cond) (ns bulti-tude.test)}
             '#{(ns bulti-tude.test)})
           (set (file->namespace-forms nil (io/file "test/bulti_tude")))))))

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
    (is (= (if *read-cond*
             '#{bulti-tude.cond bulti-tude.test}
             '#{bulti-tude.test})
           (set (namespaces-on-classpath :prefix "bulti-tude"))))))

(deftest namespace-forms-on-classpath-test
  (testing namespace-forms-on-classpath
    (is (every?
         #(= 'ns (first %))
         (namespace-forms-on-classpath)))))

(defn test-doc-from-ns-form-helper
  [docstring ns-form]
  (eval ns-form)
  (is (=  docstring
          (:doc (meta *ns*))
          (doc-from-ns-form ns-form))))

(deftest doc-from-ns-form-test
  (testing "doc-from-ns-form"
    (let [callee-ns-name (ns-name *ns*)]
      (test-doc-from-ns-form-helper
       nil
       '(ns no-doc-namespace-name))
      (test-doc-from-ns-form-helper
       "Docstring"
       '(ns regular-doc-namespace-name "Docstring"))
      (test-doc-from-ns-form-helper
       "Attribute-Docstring"
       '(ns attribute-doc-namepsace-name {:doc "Attribute-Docstring"}))
      (test-doc-from-ns-form-helper
       "Meta-Docstring"
       '(ns ^{:doc "Meta-Docstring"} meta-doc-namespace-name))
      (test-doc-from-ns-form-helper
       "Docstring"
       '(ns ^{:doc "Meta-Docstring"} meta-and-reg-doc-namespace-name "Docstring"))
      (test-doc-from-ns-form-helper
       "Attribute-Docstring"
       '(ns reg-and-attribute-doc-namespace-name "Docstring" {:doc "Attribute-Docstring"}))
      (test-doc-from-ns-form-helper
       "Attribute-Docstring"
       '(ns ^{:doc "Meta-Docstring"} all-doc-namespace-name "Docstring" {:doc "Attribute-Docstring"}))
      (in-ns callee-ns-name))))

(deftest test-invalid-namespace
  (is (= nil (ns-form-for-file (io/file "test-resources/bultitude/invalid.clj") true)))
  (is (thrown-with-msg? RuntimeException #"Map literal must contain an even number of forms"
                        (ns-form-for-file (io/file "test-resources/bultitude/invalid.clj") false))))
