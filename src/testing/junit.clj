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
  "Execute a UI step with automatic before/after screenshots.
   Takes a page object, description, and function to execute.
   Automatically captures screenshots before and after the action."
  [page description f]
  `(step ~description
     (fn []
       (let [safe-desc# (clojure.string/replace ~description #"[^a-zA-Z0-9\s-]" "")
             slug# (clojure.string/replace safe-desc# #"\s+" "-")]
         ;; Take before screenshot
         (take-screenshot ~page (str slug# "-before"))
         
         ;; Execute the step action
         (let [result# (~f)]
           ;; Take after screenshot
           (take-screenshot ~page (str slug# "-after"))
           result#)))))

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
           (let [result# (do ~@body)]
             (.testFinished event-bus#)
             
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
             
             (println (str "\n✓ Test passed: " test-name# "\n"))
             result#)
           (catch Exception e#
             (.testFailed event-bus# e#)
             
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
             (throw e#))))
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
