(ns runner.run
  (:require [clojure.string :as str])
  (:import [org.junit.platform.launcher LauncherFactory TestExecutionListener]
           [org.junit.platform.launcher.core LauncherDiscoveryRequestBuilder]
           [org.junit.platform.engine.discovery DiscoverySelectors]
           [org.junit.platform.launcher.listeners SummaryGeneratingListener]
           [runner TestRegistry])
  (:gen-class))

(defn load-test-registry
  "Loads test registry from generated TestRegistry class."
  []
  (try
    (TestRegistry/getTestRegistry)
    (catch Exception e
      (println "ERROR: Could not load test registry")
      (println "Please run compilation first: clojure -M:compile")
      (println "\nDetails:" (.getMessage e))
      (System/exit 1))))

(defn resolve-test-name
  "Resolves a test name or namespace to Java class names.
   
   Returns vector of class names to run.
   
   Examples:
     'samples.core-test/addition-test' → ['samples.core_test.AdditionTest']
     'samples.core-test' → all tests in that namespace
     nil or empty → all tests"
  [registry test-name]
  (cond
    ;; Exact test match
    (contains? registry test-name)
    [(get registry test-name)]
    
    ;; Namespace match (all tests in namespace)
    :else
    (let [matching-tests (->> registry
                              (filter (fn [[k _]]
                                        (str/starts-with? k (str test-name "/"))))
                              (map second))]
      (if (seq matching-tests)
        (vec matching-tests)
        (do
          (println (str "WARNING: Test not found: " test-name))
          (println "\nAvailable tests:")
          (doseq [test-name (sort (keys registry))]
            (println "  " test-name))
          [])))))

(defn resolve-test-names
  "Resolves multiple test names/namespaces to class names.
   If no names provided, returns all tests."
  [registry test-names]
  (if (empty? test-names)
    ;; Run all tests
    (vec (vals registry))
    ;; Run specified tests
    (distinct (mapcat #(resolve-test-name registry %) test-names))))

;; Real-time test execution listener
(defn create-console-listener []
  (reify TestExecutionListener
    (executionStarted [_ test-identifier]
      (when (.isTest test-identifier)
        (println "  ▶" (.getDisplayName test-identifier))))
    
    (executionFinished [_ test-identifier test-execution-result]
      (when (.isTest test-identifier)
        (let [status (.getStatus test-execution-result)]
          (case (str status)
            "SUCCESSFUL" (println "    ✓ PASSED")
            "FAILED"     (do
                          (println "    ✗ FAILED")
                          (when-let [ex (.getThrowable test-execution-result)]
                            (println "      " (.getMessage (.get ex)))))
            "ABORTED"    (println "    ⊘ ABORTED")
            (println "    ?" status)))))))

(defn run-junit-tests
  "Runs JUnit tests via Platform Launcher with real-time output."
  [class-names]
  (let [launcher (LauncherFactory/create)
        summary-listener (SummaryGeneratingListener.)
        console-listener (create-console-listener)
        
        selectors (mapv #(DiscoverySelectors/selectClass %) class-names)
        
        request (-> (LauncherDiscoveryRequestBuilder/request)
                    (.selectors (into-array selectors))
                    (.build))]
    
    (println "\n=== Running Tests ===\n")
    (println "Running" (count class-names) "test(s)...\n")
    
    (.execute launcher request 
              (into-array TestExecutionListener [summary-listener console-listener]))
    
    ;; Print summary
    (let [summary (.getSummary summary-listener)]
      (println "\n=== Test Summary ===")
      (printf "Tests run: %d\n" (.getTestsStartedCount summary))
      (printf "Passed: %d\n" (.getTestsSucceededCount summary))
      (printf "Failed: %d\n" (.getTestsFailedCount summary))
      (printf "Skipped: %d\n" (.getTestsSkippedCount summary))
      
      (when (pos? (.getTestsFailedCount summary))
        (println "\nFailures:")
        (doseq [failure (.getFailures summary)]
          (println "  -" (-> failure .getTestIdentifier .getDisplayName))
          (println "    " (-> failure .getException .getMessage))))
      
      (if (pos? (.getTestsFailedCount summary))
        (System/exit 1)
        (System/exit 0)))))

(defn -main
  "Runs JUnit tests. Accepts optional test names or namespaces.
   
   Examples:
     clojure -M:run
     clojure -M:run samples.core-test/addition-test
     clojure -M:run samples.core-test
     clojure -M:run samples.core-test/test1 samples.login-test/test2"
  [& args]
  (println "=== Clojure JUnit Test Runner ===\n")
  
  (let [registry (load-test-registry)
        test-names args
        class-names (resolve-test-names registry test-names)]
    
    (when (empty? class-names)
      (println "ERROR: No tests to run")
      (System/exit 1))
    
    (println "Resolved tests:")
    (doseq [class-name class-names]
      (let [test-entry (->> registry
                            (filter #(= class-name (val %)))
                            first)]
        (println "  " (key test-entry) "→" class-name)))
    
    (run-junit-tests class-names)))
