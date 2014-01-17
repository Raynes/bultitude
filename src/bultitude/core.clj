(ns bultitude.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string]
            [dynapath.util :as dp])
  (:import (java.util.jar JarFile JarEntry)
           (java.util.zip ZipException)
           (java.io File BufferedReader PushbackReader InputStreamReader)
           (clojure.lang DynamicClassLoader)))

(declare namespace-forms-in-dir
         file->namespace-forms)

(defn- clj? [^File f]
  (and (not (.isDirectory f))
       (.endsWith (.getName f) ".clj")))

(defn- clj-jar-entry? [^JarEntry f]
  (and (not (.isDirectory f))
       (.endsWith (.getName f) ".clj")))

(defn- jar? [^File f]
  (and (.isFile f) (.endsWith (.getName f) ".jar")))

(defn- read-ns-form
  "Given a reader on a Clojure source file, read until an ns form is found."
  [rdr]
  (let [form (try (read rdr false ::done)
                  (catch Exception e ::done))]
    (if (try
          (and (list? form) (= 'ns (first form)))
          (catch Exception _))
      (try
        (str form) ;; force the read to read the whole form, throwing on error
        form
        (catch Exception _))
      (when-not (= ::done form)
        (recur rdr)))))

(defn ns-form-for-file [file]
  (with-open [r (PushbackReader. (io/reader file))] (read-ns-form r)))

(defn namespaces-in-dir
  "Return a seq of all namespaces found in Clojure source files in dir."
  [dir]
  (map second (namespace-forms-in-dir dir)))

(defn namespace-forms-in-dir
  "Return a seq of all namespace forms found in Clojure source files in dir."
  [dir]
  (for [^File f (file-seq (io/file dir))
        :when (and (clj? f) (.canRead f))
        :let [ns-form (ns-form-for-file f)]
        :when ns-form]
    ns-form))

(defn- ns-form-in-jar-entry [^JarFile jarfile ^JarEntry entry]
  (with-open [rdr (-> jarfile
                      (.getInputStream entry)
                      InputStreamReader.
                      BufferedReader.
                      PushbackReader.)]
    (read-ns-form rdr)))

(defn- namespace-forms-in-jar [^File jar]
  (try
    (let [jarfile (JarFile. jar)]
      (for [entry (enumeration-seq (.entries jarfile))
            :when (clj-jar-entry? entry)
            :let [ns-form (ns-form-in-jar-entry jarfile entry)]
            :when ns-form]
        ns-form))
    (catch ZipException e
      (throw (Exception. (str "jar file corrupt: " jar) e)))))

(defn- split-classpath [^String classpath]
  (.split classpath (System/getProperty "path.separator")))

(defn loader-classpath
  "Returns a sequence of File objects from a classloader."
  [loader]
  (map io/as-file (dp/classpath-urls loader)))

(defn classpath-files
  "Returns a sequence of File objects of the elements on the classpath."
  ([classloader]
     (map io/as-file (dp/all-classpath-urls classloader)))
  ([] (classpath-files (clojure.lang.RT/baseLoader))))

(defn- classpath->collection [classpath]
  (if (coll? classpath)
    classpath
    (split-classpath classpath)))

(defn- classpath->files [classpath]
  (map io/file classpath))

(defn file->namespaces
  "Map a classpath file to the namespaces it contains. `prefix` allows for
   reducing the namespace search space. For large directories on the classpath,
   passing a `prefix` can provide significant efficiency gains."
  [^String prefix ^File f]
  (map second (file->namespace-forms prefix f)))

(defn file->namespace-forms
  "Map a classpath file to the namespace forms it contains. `prefix` allows for
   reducing the namespace search space. For large directories on the classpath,
   passing a `prefix` can provide significant efficiency gains."
  [^String prefix ^File f]
  (cond
    (.isDirectory f) (namespace-forms-in-dir
                      (if prefix
                        (io/file f (-> prefix
                                       (.replaceAll "\\." "/")
                                       (.replaceAll "-" "_")))
                        f))
    (jar? f) (let [ns-list (namespace-forms-in-jar f)]
               (if prefix
                 (filter #(and (second %) (.startsWith (name (second %)) prefix)) ns-list)
                 ns-list))))


(defn namespace-forms-on-classpath
  "Returs the namespaces forms matching the given prefix both on disk and
  inside jar files. If :prefix is passed, only return namespaces that begin with
  this prefix. If :classpath is passed, it should be a seq of File objects or a
  classpath string. If it is not passed, default to java.class.path and the
  current classloader, assuming it is a dynamic classloader."
  [& {:keys [prefix classpath] :or {classpath (classpath-files)}}]
  (mapcat
   (partial file->namespace-forms prefix)
   (->> classpath
        classpath->collection
        classpath->files)))

(defn namespaces-on-classpath
  "Return symbols of all namespaces matching the given prefix both on disk and
  inside jar files. If :prefix is passed, only return namespaces that begin with
  this prefix. If :classpath is passed, it should be a seq of File objects or a
  classpath string. If it is not passed, default to java.class.path and the
  current classloader, assuming it is a dynamic classloader."
  [& args]
  (map second (apply namespace-forms-on-classpath args)))

(defn path-for
  "Transform a namespace into a .clj file path relative to classpath root."
  [namespace]
  (str (-> (str namespace)
           (.replace \- \_)
           (.replace \. \/))
       ".clj"))

(defn doc-from-ns-form
  "Extract the docstring from a given ns form without evaluating the form. The docstring returned should be the return value of (:doc (meta namespace-symbol)) if the ns-form were to be evaluated."
  [ns-form]
  (:doc (meta (second (second (second (macroexpand ns-form)))))))
