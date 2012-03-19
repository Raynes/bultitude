(ns bultitude.core
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import (java.util.jar JarFile)
           (java.io File BufferedReader PushbackReader InputStreamReader)
           (clojure.lang DynamicClassLoader)))

(defn- clj? [f]
  ;; Needs to work on JarEntries and Files, the former of which has no .isFile
  (and (not (.isDirectory f)) (.endsWith (.getName f) ".clj")))

(defn- jar? [f]
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
        (second form)
        (catch Exception _))
      (when-not (= ::done form)
        (recur rdr)))))

(defn namespaces-in-dir
  "Return a seq of all namespaces found in Clojure source files in dir."
  [dir]
  (for [f (file-seq (io/file dir))
        :when (clj? f)
        :let [ns-form (read-ns-form (PushbackReader. (io/reader f)))]
        :when ns-form]
    ns-form))

(defn- ns-in-jar-entry [jarfile entry]
  (with-open [rdr (-> jarfile
                      (.getInputStream (.getEntry jarfile (.getName entry)))
                      InputStreamReader.
                      BufferedReader.
                      PushbackReader.)]
    (read-ns-form rdr)))

(defn- namespaces-in-jar [jar]
  (let [jarfile (JarFile. jar)]
    (for [entry (enumeration-seq (.entries jarfile))
          :when (clj? entry)
          :let [ns-form (ns-in-jar-entry jarfile entry)]
          :when ns-form]
      ns-form)))

(defn- split-classpath [classpath]
  (.split classpath (System/getProperty "path.separator")))

(defn loader-classpath
  "Returns a sequence of File paths from a classloader."
  [loader]
  (when (instance? java.net.URLClassLoader loader)
    (map
     #(java.io.File. (.getPath ^java.net.URL %))
     (.getURLs ^java.net.URLClassLoader loader))))

(defn classpath-files
  "Returns a sequence of File objects of the elements on the classpath."
  ([classloader]
     (distinct
      (mapcat
       loader-classpath
       (reverse
        (take-while
         identity
         (iterate #(.getParent %) classloader))))))
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
  [prefix f]
  (cond
    (.isDirectory f) (namespaces-in-dir
                      (if prefix
                        (io/file f (-> prefix
                                       (.replaceAll "\\." "/")
                                       (.replaceAll "-" "_")))
                        f))
    (jar? f) (let [ns-list (namespaces-in-jar f)]
               (if prefix
                 (filter #(and % (.startsWith (name %) prefix)) ns-list)
                 ns-list))))

(defn namespaces-on-classpath
  "Return symbols of all namespaces matching the given prefix both on disk and
  inside jar files. If :prefix is passed, only return namespaces that begin with
  this prefix. If :classpath is passed, it should be a seq of File objects or a
  classpath string. If it is not passed, default to java.class.path and the
  current classloader, assuming it is a dynamic classloader."
  [& {:keys [prefix classpath] :or {classpath (classpath-files)}}]
  (mapcat
   (partial file->namespaces prefix)
   (->> classpath classpath->collection classpath->files (filter #(.canRead %)))))

(defn path-for
  "Transform a namespace into a .clj file path relative to classpath root."
  [namespace]
  (str (-> (str namespace)
           (.replace \- \_)
           (.replace \. \/))
       ".clj"))
