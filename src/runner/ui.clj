(ns runner.ui
  (:require [runner.factory]
            [runner.gen :as gen])
  (:import [org.junit.platform.launcher TestExecutionListener LauncherDiscoveryRequest]
           [org.junit.platform.launcher.core LauncherFactory LauncherDiscoveryRequestBuilder]
           [org.junit.platform.engine.discovery DiscoverySelectors]
           [org.junit.platform.launcher.listeners SummaryGeneratingListener TestExecutionSummary$Failure]
           [runner TestRegistry])
  (:gen-class))

(defn -main
  "Command-line entry point to run Clojure tests via JUnit 5 Platform.
  Accepts --tests \"ns1,ns2/fn1,ns3/fn2,...\"
  Generates ChainTest HTML reports.
  Example: clojure -M -m my-project.test-runner --tests my-app.core-tests,my-app.utils-tests/specific-util-test"
  [& args]
  (let [launcher (LauncherFactory/create)
        summary-listener (SummaryGeneratingListener.)
        ^LauncherDiscoveryRequest request

        (-> (LauncherDiscoveryRequestBuilder/request)
            (.selectors (into-array (mapv
                                     (fn [x]
                                       (DiscoverySelectors/selectPackage (str x)))
                                     (TestRegistry/getPackages))))
            (.build))]

    (println "Discovering JUnit tests from runner.ClojureTestFactory...")

            ;; 4. Register listeners
            ;; The summary listener is for console summary.
            ;; The ChainTest listener handles its own report generation.

            ;; Removed LegacyXmlReportGeneratingListener and its LauncherSessionListener logic

    (println "Executing tests via JUnit Platform Launcher...")
            ;; 5. Execute the tests
    (.execute launcher request (into-array TestExecutionListener [summary-listener]))

            ;; 6. Process results and set exit code (from summary-listener for console)
    (let [summary (.getSummary summary-listener)]
      (println "\n--- JUnit Test Run Console Summary ---")
      (printf "  Tests Found: %d\n" (.getTestsFoundCount summary))
      (printf "  Tests Started: %d\n" (.getTestsStartedCount summary))
      (printf "  Tests Succeeded: %d\n" (.getTestsSucceededCount summary))
      (printf "  Tests Failed: %d\n" (.getTestsFailedCount summary))
      (printf "  Tests Skipped: %d\n" (.getTestsSkippedCount summary))
      (printf "  Tests Aborted: %d\n" (.getTestsAbortedCount summary))
      (printf "  Total Time: %d ms\n" (.getTimeFinished summary))

      (when-let [failures (seq (.getFailures summary))]
        (println "\nFailures (Console Summary):")
        (doseq [^TestExecutionSummary$Failure failure failures]
          (println (str "  Test: " (-> failure .getTestIdentifier .getDisplayName)))
          (println (str "    Reason: " (-> failure .getException .getMessage)))))
      (if (pos? (.getTestsFailedCount summary))
        (System/exit 1)
        (System/exit 0)))))

(println "Expangind")
(println (macroexpand-1
          '(gen/generate-runner-classes
            'samples.test1
            'samples.test2)))

(println "Compling")
(gen/generate-runner-classes
 'samples.test1
 'samples.test2)




