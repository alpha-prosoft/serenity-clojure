(ns testing.junit
  "Serenity BDD integration with Playwright for Clojure tests.
   Provides macros for test lifecycle management with automated reporting."
  (:require [clojure.test :as t]
            [clojure.java.io :as io])
  (:import [net.serenitybdd.rest SerenityRest]
           [com.microsoft.playwright Playwright BrowserType$LaunchOptions Page Page$ScreenshotOptions]
           [java.nio.file Paths]
           [java.io File]
           [net.thucydides.core.steps StepEventBus BaseStepListener]
           [net.thucydides.model.steps ExecutedStepDescription StepFailure]
           [net.thucydides.model.domain Story]
           [net.thucydides.model.reports.json JSONTestOutcomeReporter]
           [net.serenitybdd.core Serenity]
           [net.serenitybdd.cli.reporters CLIAggregateReportGenerator]))

;; Screenshot counter for unique filenames
(def screenshot-counter (atom 0))

(defn format-stack-trace
  "Format exception stack trace as a string"
  [^Throwable e]
  (let [sw (java.io.StringWriter.)
        pw (java.io.PrintWriter. sw)]
    (.printStackTrace e pw)
    (.toString sw)))

(defn ensure-dir [path]
  "Ensure directory exists, create if not"
  (let [dir (io/file path)]
    (when-not (.exists dir)
      (.mkdirs dir))))

(defn take-screenshot
  "Capture screenshot and attach to current step.
   Returns the filename of the saved screenshot."
  [^Page page description]
  (try
    (let [bus (StepEventBus/getEventBus)
          timestamp (System/currentTimeMillis)
          counter (swap! screenshot-counter inc)
          filename (str (clojure.string/replace description #"\s+" "_") 
                       "-" timestamp "-" counter ".png")
          screenshot-bytes (.screenshot page)]
      
      ;; Register screenshot with StepEventBus - it handles file writing and attachment
      (.recordScreenshot bus filename screenshot-bytes)
      
      (println (str "📸 Screenshot attached: " filename))
      filename)
    (catch Exception e
      (println (str "Failed to take screenshot: " (.getMessage e)))
      nil)))

(defn init-serenity []
  "Initialize Serenity BDD environment and return the listener"
  (let [event-bus (StepEventBus/getEventBus)
        output-dir (File. (or (System/getProperty "serenity.outputDirectory")
                              "target/site/serenity"))
        listener (BaseStepListener. output-dir)]
    (.registerListener event-bus listener)
    listener))

(defmacro step
  "Execute a test step with automatic Serenity reporting.
   Description will appear in the generated reports."
  [description f]
  `(let [event-bus# (StepEventBus/getEventBus)
         step-desc# (ExecutedStepDescription/withTitle ~description)
         start-time# (System/currentTimeMillis)]
     (try
       (println (str "▶ Step: " ~description))
       (.stepStarted event-bus# step-desc#)
       (let [result# (~f)]
         (.stepFinished event-bus#)
         (println (str "✓ Step completed (" (- (System/currentTimeMillis) start-time#) "ms)"))
         result#)
       (catch Exception e#
         (let [failure# (StepFailure. step-desc# e#)]
           (.stepFailed event-bus# failure#))
         (println (str "✗ Step failed: " (.getMessage e#)))
         (throw e#)))))

(defmacro api-step
  "Execute an API step with REST Assured logging.
   Automatically wraps SerenityRest calls with proper reporting."
  [description f]
  `(step ~description
     (fn []
       (try
         (let [result# (~f)]
           (SerenityRest/reset)
           result#)
         (catch Exception e#
           (SerenityRest/reset)
           (throw e#))))))

(defmacro ui-step
  "Execute a UI step with automatic before/after screenshots using nested child steps.
   Takes a page object, description, and function to execute.
   Creates a parent step with a wrapper step containing both Before and After screenshots."
  [page description f]
  `(let [event-bus# (StepEventBus/getEventBus)
         step-desc# (ExecutedStepDescription/withTitle ~description)
         start-time# (System/currentTimeMillis)]
     (try
       (println (str "▶ Step: " ~description))
       (.stepStarted event-bus# step-desc#)
       
       (let [safe-desc# (clojure.string/replace ~description #"[^a-zA-Z0-9\s-]" "")
             slug# (clojure.string/replace safe-desc# #"\s+" "-")
             before-step# (ExecutedStepDescription/withTitle (str ~description " - Before"))
             after-step# (ExecutedStepDescription/withTitle (str ~description " - After"))]
         
         ;; Start Before step
         (.stepStarted event-bus# before-step#)
         (take-screenshot ~page (str slug# "-before"))
         
         ;; Start After step while Before is still active
         ;; This makes After a child of Before (nested one level deeper)
         (.stepStarted event-bus# after-step#)
         
         ;; Execute the main action between the two screenshots
         (let [result# (~f)]
           (take-screenshot ~page (str slug# "-after"))
           
           ;; Finish After first (it's the deepest in the stack)
           (.stepFinished event-bus#)
           
           ;; Then finish Before
           (.stepFinished event-bus#)
           
           ;; Finally finish parent
           (.stepFinished event-bus#)
           (println (str "✓ Step completed (" (- (System/currentTimeMillis) start-time#) "ms)"))
           result#))
       (catch Exception e#
         (let [failure# (StepFailure. step-desc# e#)]
           (.stepFailed event-bus# failure#))
         (println (str "✗ Step failed: " (.getMessage e#)))
         (throw e#)))))

(defn start-browser []
  "Start Playwright browser and return page instance"
  (let [pw (Playwright/create)
        browser (.launch (.chromium pw) 
                        (-> (BrowserType$LaunchOptions.)
                            (.setHeadless true)))
        context (.newContext browser)
        page (.newPage context)]
    {:playwright pw
     :browser browser
     :context context
     :page page}))

(defn stop-browser [browser-map]
  "Close browser and Playwright instance"
  (try
    (.close (:page browser-map))
    (.close (:context browser-map))
    (.close (:browser browser-map))
    (.close (:playwright browser-map))
    (catch Exception e
      (println "Warning: Error closing browser:" (.getMessage e)))))

(defmacro with-serenity
  "Main test wrapper that provides Playwright page and Serenity integration.
   
   Usage:
     (deftest my-test
       (with-serenity [page]
         (step \"Navigate to site\" 
           #(.navigate page \"https://example.com\"))
         (step \"Take screenshot\"
           #(take-screenshot page \"homepage\"))))"
  [[page-binding] & body]
  `(let [browser-map# (start-browser)
         ~page-binding (:page browser-map#)
         event-bus# (StepEventBus/getEventBus)
         story# (Story/withIdAndPathAndFeature 
                  "samples" 
                  "Samples" 
                  "samples" 
                  "samples" 
                  "Sample Tests")
         test-name# (or (some-> t/*testing-vars* first meta :name str)
                       "Unknown Test")]
     (try
       (let [listener# (init-serenity)]
         (.testStarted event-bus# test-name# story#)
          (println (str "\n" (apply str (repeat 60 "=")) "\n"
                       "Test: " test-name# "\n"
                       (apply str (repeat 60 "=")) "\n"))
          (try
            ;; Track assertion failures
            (let [failures# (atom [])
                  original-report# t/report]
              ;; Intercept clojure.test reports to capture failures
              (with-redefs [t/report (fn [m#]
                                      (when (#{:fail :error} (:type m#))
                                        (swap! failures# conj m#))
                                      (original-report# m#))]
                (let [result# (do ~@body)
                      has-failures# (seq @failures#)]
                  
                  ;; Report to Serenity based on test outcome
                  (if has-failures#
                    (let [failure-msg# (apply str
                                             (concat
                                              [(str "Test had " (count @failures#) " assertion failure(s)\n\n")]
                                              (mapcat (fn [failure#]
                                                       [(str "• " (or (:message failure#) "Assertion failed") "\n")
                                                        (when (:expected failure#)
                                                          (str "  Expected: " (pr-str (:expected failure#)) "\n"))
                                                        (when (:actual failure#)
                                                          (str "  Actual:   " (pr-str (:actual failure#)) "\n"))
                                                        (when (and (:file failure#) (:line failure#))
                                                          (str "  Location: " (:file failure#) ":" (:line failure#) "\n"))
                                                        ;; Add stack trace if the actual value is a throwable
                                                        (when-let [actual-val# (:actual failure#)]
                                                          (when (instance? Throwable actual-val#)
                                                            (str "\n=== Stack Trace ===\n" (format-stack-trace actual-val#) "\n")))
                                                        "\n"])
                                                     @failures#)))
                          failure-ex# (Exception. failure-msg#)]
                      (.testFailed event-bus# failure-ex#))
                    (.testFinished event-bus#))
                  
                  ;; Write JSON report
                  (try
                    (let [outcome# (.getCurrentTestOutcome listener#)]
                      (when outcome#
                        (let [output-dir# (File. (or (System/getProperty "serenity.outputDirectory")
                                                     "target/site/serenity"))
                              reporter# (JSONTestOutcomeReporter.)]
                          (ensure-dir (.getPath output-dir#))
                          (.setOutputDirectory reporter# output-dir#)
                          (.generateReportFor reporter# outcome#))))
                    (catch Exception e#
                      (println "Warning: Could not write JSON report:" (.getMessage e#))))
                  
                  ;; Print result summary
                  (if has-failures#
                    (do
                      (println (str "\n✗ Test failed: " test-name# "\n"))
                      (println "Assertion Failures:")
                      (doseq [failure# @failures#]
                        (println (str "  • " (or (:message failure#) "Assertion failed")))
                        (when (:expected failure#)
                          (println (str "    Expected: " (pr-str (:expected failure#)))))
                        (when (:actual failure#)
                          (println (str "    Actual:   " (pr-str (:actual failure#)))))
                        (when (and (:file failure#) (:line failure#))
                          (println (str "    Location: " (:file failure#) ":" (:line failure#))))
                        (println)))
                    (println (str "\n✓ Test passed: " test-name# "\n")))
                  
                  result#)))
           (catch Exception e#
             ;; Create enhanced exception with full stack trace for reporting
             (let [stack-trace# (format-stack-trace e#)
                   enhanced-msg# (str (.getMessage e#) "\n\n=== Full Stack Trace ===\n" stack-trace#)
                   enhanced-ex# (Exception. enhanced-msg# e#)]
               (.testFailed event-bus# enhanced-ex#)
               
               ;; Write JSON report even on failure
               (try
                 (let [outcome# (.getCurrentTestOutcome listener#)]
                   (when outcome#
                     (let [output-dir# (File. (or (System/getProperty "serenity.outputDirectory")
                                                  "target/site/serenity"))
                           reporter# (JSONTestOutcomeReporter.)]
                       (ensure-dir (.getPath output-dir#))
                       (.setOutputDirectory reporter# output-dir#)
                       (.generateReportFor reporter# outcome#))))
                 (catch Exception e2#
                   (println "Warning: Could not write JSON report:" (.getMessage e2#))))
               
               (println (str "\n✗ Test failed: " test-name# "\n"))
               (println "Error message:")
               (println (.getMessage e#))
               (println "\nFull stack trace:")
               (println stack-trace#)
               (throw e#)))))
       (finally
         (stop-browser browser-map#)
         (.dropAllListeners event-bus#)))))

(defn generate-reports 
  "Generate HTML reports from JSON test results using Serenity CLI"
  []
  (let [output-dir-str (or (System/getProperty "serenity.outputDirectory") 
                           "target/site/serenity")
        output-dir (File. output-dir-str)
        source-path (Paths/get output-dir-str (into-array String []))
        project-name (or (System/getProperty "serenity.project.name") "Serenity Clojure")]
    (ensure-dir output-dir-str)
    
    (println (str "\n========================================"))
    (println (str "Generating Serenity Reports"))
    (println (str "========================================"))
    (println (str "  Source: " output-dir-str))
    (println (str "  Project: " project-name))
    
    (try
      (let [generator (CLIAggregateReportGenerator. source-path source-path "" "" "" "" "" "" "" "")]
        (.generateReportsFrom generator source-path))
      (println (str "  ✓ HTML reports generated"))
      (println (str "  Open: " output-dir-str "/index.html"))
      (println (str "========================================\n"))
      (catch Exception e
        (println (str "  ✗ Error generating reports: " (.getMessage e)))
        (println (str "========================================\n"))
        (throw e)))))
