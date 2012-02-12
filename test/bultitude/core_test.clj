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
         (set (namespaces-on-classpath :prefix "bultitude"))))))
