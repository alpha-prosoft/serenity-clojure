(ns runner.gen
  (:require [clojure.string :as string]
            [clojure.test :as test]
            [clojure.java.io :as io])
  (:import [org.junit.jupiter.api TestInfo]))

(def ^:dynamic *test-info*
  "Dynamic var to hold the current JUnit TestInfo object."
  nil)

;; Compile-time registry to track generated tests
(def compile-time-registry (atom {}))

(defn clojure-ns-to-java-package
  "Converts Clojure namespace to Java package name.
   Hyphens → underscores"
  [ns-sym]
  (string/replace (str ns-sym) #"-" "_"))

(defn test-var-to-java-class-name
  "Converts test var name to Java class name.
   Examples: addition-test → AdditionTest"
  [test-var-sym]
  (let [test-name (name test-var-sym)
        parts (string/split test-name #"-")]
    (apply str (map string/capitalize parts))))

(defn test-var-to-fq-class-name
  "Returns fully qualified Java class name for a Clojure test."
  [ns-sym test-var-sym]
  (str (clojure-ns-to-java-package ns-sym) 
       "." 
       (test-var-to-java-class-name test-var-sym)))

(defmacro defjunit
  "Defines a Clojure test that also generates a JUnit test class.
   
   Works like deftest but also:
   - Generates a JUnit test class at compile-time
   - Registers the test for JUnit execution
   - Works in REPL/IDE environments
   
   Usage:
     (defjunit addition-test
       (is (= 4 (+ 2 2)))
       (is (= 0 (+ 0 0))))
   
   The macro creates:
   1. A clojure.test deftest (for REPL/IDE)
   2. A JUnit test class (for JUnit Platform execution)
   3. An entry in the test registry"
  [test-name & body]
  (let [current-ns-sym (ns-name *ns*)
        class-name (test-var-to-fq-class-name current-ns-sym test-name)
        impl-fn-name (symbol (str "-" (test-var-to-java-class-name test-name) "Impl"))
        test-var-fqn (symbol (str current-ns-sym "/" test-name))
        display-name (str current-ns-sym "/" test-name)
        class-sym (symbol class-name)
        test-annotation 'org.junit.jupiter.api.Test
        display-annotation 'org.junit.jupiter.api.DisplayName
        testinfo-class 'org.junit.jupiter.api.TestInfo]
    
    `(do
       ;; 1. Define the test var (like deftest)
       (clojure.test/deftest ~(with-meta test-name {:junit true})
         ~@body)
       
       ;; 2. Generate JUnit class (compile-time and runtime)
       (try
         ;; Compile the namespace (required for gen-class)
         (compile '~current-ns-sym)
         
         ;; Create implementation function
         (intern '~current-ns-sym '~impl-fn-name
                 (fn [this# test-info#]
                   (binding [runner.gen/*test-info* test-info#]
                     (clojure.test/test-var (resolve '~test-var-fqn)))))
         
         ;; Generate JUnit class
         (let [gen-form# (list 'gen-class
                              :name '~class-sym
                              :impl-ns '~current-ns-sym
                              :methods [[(with-meta 'runTest
                                           {(symbol "org.junit.jupiter.api.Test") {}
                                            (symbol "org.junit.jupiter.api.DisplayName") ~display-name})
                                         [(symbol "org.junit.jupiter.api.TestInfo")] 'void]])]
           (eval gen-form#))
         
         ;; Register in compile-time registry
         (swap! runner.gen/compile-time-registry assoc 
                ~(str current-ns-sym "/" test-name)
                ~class-name)
         
         ;; Print progress during compile phase
         (println "  ✓" ~class-name)
         
         (catch Exception e#
           ;; In REPL mode, class generation may fail - that's ok
           nil)))))

(defn get-compile-time-registry
  "Returns the compile-time registry of all defjunit tests.
   Called by compile.clj to generate TestRegistry class."
  []
  @compile-time-registry)

;; Auto-discovery functions
(defn find-clj-files
  "Recursively finds all .clj files in given directory paths."
  [paths]
  (doseq [path paths]
    (when-not (.exists (io/file path))
      (throw (Exception. (str "Test directory '" path "' does not exist")))))
  (->> paths
       (mapcat #(file-seq (io/file %)))
       (filter #(.isFile %))
       (filter #(.endsWith (.getName %) ".clj"))))

(defn extract-ns-from-file
  "Extracts namespace symbol from .clj file."
  [file]
  (try
    (with-open [rdr (java.io.PushbackReader. 
                     (io/reader file))]
      (loop []
        (when-let [form (read rdr false nil)]
          (if (and (seq? form) (= 'ns (first form)))
            (second form)
            (recur)))))
    (catch Exception e
      (throw (Exception. 
              (str "Failed to parse " (.getName file) ": " (.getMessage e)) e)))))

(defn discover-test-namespaces
  "Auto-discovers test namespaces by scanning directories."
  ([test-paths]
   (->> (find-clj-files test-paths)
        (map extract-ns-from-file)
        (remove nil?)
        (distinct)))
  ([] (discover-test-namespaces ["test"])))

(defn generate-test-registry-class
  "Generates TestRegistry class with static method returning test map.
   Called after all test files have been loaded/expanded."
  []
  (let [registry-map (get-compile-time-registry)]
    
    ;; Intern the function that returns the registry
    (intern 'runner.gen '-getTestRegistry
            (fn [] registry-map))
    
    ;; Generate the class
    (eval
      `(gen-class
         :name runner.TestRegistry
         :impl-ns runner.gen
         :methods [^:static [getTestRegistry [] java.util.Map]]))
    
    registry-map))


