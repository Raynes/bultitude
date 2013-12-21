(ns bultitude.core-test
  (:use clojure.test
        bultitude.core))

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
