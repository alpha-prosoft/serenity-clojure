(ns runner.compile
  (:require [runner.gen :as gen]
            [clojure.java.io :as io])
  (:gen-class))

(defn compile-all-tests
  "Discovers and compiles all test namespaces.
   This triggers defjunit macro expansion and class generation."
  [test-paths]
  (println "=== Clojure JUnit Test Compilation ===\n")
  (println "Scanning directories:" test-paths)
  
  ;; Discover test namespaces
  (let [namespaces (gen/discover-test-namespaces test-paths)]
    (println "Found" (count namespaces) "namespaces:")
    (doseq [ns-sym (sort namespaces)]
      (println "  -" ns-sym))
    
    (println "\nLoading test files and generating JUnit classes...")
    
    ;; Load each namespace - this triggers defjunit macro expansion
    (doseq [ns-sym namespaces]
      (try
        (require ns-sym :reload)
        (catch Exception e
          (println "ERROR: Failed to load" ns-sym)
          (throw e))))
    
    ;; Generate TestRegistry class
    (println "\nGenerating TestRegistry...")
    (let [registry (gen/generate-test-registry-class)]
      (println "  ✓ runner.TestRegistry")
      
      (println "\n=== Compilation Complete ===")
      (println "Total tests:" (count registry))
      
      (when (pos? (count registry))
        (println "\nGenerated tests:")
        (doseq [[test-name class-name] (sort registry)]
          (println "  " test-name "→" class-name))))))

(defn -main
  "Compiles all defjunit tests and generates test registry.
   
   Args:
     test-paths - Optional test directory paths (default: test/)
   
   Examples:
     clojure -M:compile
     clojure -M:compile test integration"
  [& args]
  (let [test-paths (if (seq args) (vec args) ["test"])]
    (try
      (compile-all-tests test-paths)
      (System/exit 0)
      (catch Exception e
        (println "\nERROR:" (.getMessage e))
        (println "\nStack trace:")
        (.printStackTrace e)
        (System/exit 1)))))

