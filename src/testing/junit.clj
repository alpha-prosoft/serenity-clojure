(ns testing.junit
  "Serenity BDD integration with Playwright for Clojure tests.
   Provides macros for test lifecycle management with automated reporting."
  (:require [clojure.test :as t]
            [clojure.java.io :as io])
  (:import [net.serenitybdd.rest SerenityRest]
           [com.microsoft.playwright Playwright BrowserType$LaunchOptions Page]
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
  (let [output-dir (or (System/getProperty "serenity.outputDirectory") 
                       "target/site/serenity")
        timestamp (System/currentTimeMillis)
        counter (swap! screenshot-counter inc)
        filename (str (clojure.string/replace description #"\s+" "_") 
                     "-" timestamp "-" counter ".png")
        filepath (str output-dir "/" filename)]
    (ensure-dir output-dir)
    (.screenshot page (into-array [(Paths/get filepath (into-array String []))]))
    (println (str "📸 Screenshot saved: " filename))
    
    ;; Record screenshot in Serenity
    (try
      (let [screenshot-bytes (java.nio.file.Files/readAllBytes 
                               (Paths/get filepath (into-array String [])))]
        (.recordScreenshot (.getStepEventBus (StepEventBus/getParallelEventBus)) 
                          screenshot-bytes))
      (catch Exception e
        (println "Warning: Could not record screenshot in Serenity:" (.getMessage e))))
    
    filename))

(defn init-serenity []
  "Initialize Serenity BDD environment"
  (let [event-bus (StepEventBus/getParallelEventBus)]
    (.registerListener event-bus (BaseStepListener. 
                                    (or (System/getProperty "serenity.outputDirectory")
                                        "target/site/serenity")))))

(defmacro step
  "Execute a test step with automatic Serenity reporting.
   Description will appear in the generated reports."
  [description f]
  `(let [event-bus# (StepEventBus/getParallelEventBus)
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
         (let [failure# (StepFailure/forException e#)]
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
         event-bus# (StepEventBus/getParallelEventBus)
         story# (Story/withIdAndPathAndFeature 
                  "samples" 
                  "Samples" 
                  "samples" 
                  "samples" 
                  "Sample Tests")
         test-name# (or (some-> t/*testing-vars* first meta :name str)
                       "Unknown Test")]
     (try
       (init-serenity)
       (.testStarted event-bus# test-name# story#)
       (println (str "\n" (apply str (repeat 60 "=")) "\n"
                    "Test: " test-name# "\n"
                    (apply str (repeat 60 "=")) "\n"))
       (let [result# (do ~@body)]
         (.testFinished event-bus#)
         (println (str "\n✓ Test passed: " test-name# "\n"))
         result#)
       (catch Exception e#
         (.testFailed event-bus# e#)
         (println (str "\n✗ Test failed: " test-name# "\n"))
         (throw e#))
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
        dest-path (Paths/get output-dir-str (into-array String []))
        project-name (or (System/getProperty "serenity.project.name") "Serenity Clojure")]
    (ensure-dir output-dir-str)
    
    (println (str "\n========================================"))
    (println (str "Generating Serenity Reports"))
    (println (str "========================================"))
    (println (str "  Source: " output-dir-str))
    (println (str "  Project: " project-name))
    
    (try
      (let [generator (CLIAggregateReportGenerator. project-name "" "" "" "" "" "" "")]
        (.generateReportsFrom generator source-path dest-path))
      (println (str "  ✓ HTML reports generated"))
      (println (str "  Open: " output-dir-str "/index.html"))
      (println (str "========================================\n"))
      (catch Exception e
        (println (str "  ✗ Error generating reports: " (.getMessage e)))
        (println (str "========================================\n"))
        (throw e)))))
