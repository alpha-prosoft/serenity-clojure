(ns runner.factory
  (:require [clojure.string :as str]
            [clojure.test :as t])
  (:import [org.junit.jupiter.api DynamicTest]
           [org.junit.jupiter.api.function Executable]
           [org.opentest4j AssertionFailedError])
  (:gen-class
   :name ^{org.junit.jupiter.api.extension.ExtendWith
           [com.aventstack.chaintest.plugins.ChainTestExecutionCallback]} runner.ClojureTestFactory
   :constructors {[] []}
   :methods [[^{org.junit.jupiter.api.TestFactory {}}
              createClojureTests [] java.util.Collection]]))

(defn- parse-and-prepare-target
  [target-str]
  (println (str "[TestFactory] Processing target: " target-str))
  (if (str/includes? target-str "/")
    (try
      (let [target-sym (symbol target-str)
            ns-sym (symbol (namespace target-sym))]
        (if (nil? ns-sym)
          (do (println (str "[TestFactory] Warning: Invalid target format (missing namespace?): " target-str)) nil)
          (do
            (require ns-sym)
            (if-let [test-var (resolve target-sym)]
              (if (:test (meta test-var))
                {:test-type :var :id test-var :display-name (str "Clojure Var: " target-str)}
                (do (println (str "[TestFactory] Warning: " target-str " resolved but not a deftest.")) nil))
              (do (println (str "[TestFactory] Warning: Could not resolve test var: " target-str)) nil)))))
      (catch Throwable e ; Catch Throwable for class loading issues etc.
        (println (str "[TestFactory] Error resolving var '" target-str "': " (.getMessage e)))
        (.printStackTrace e)
        nil))
    (try
      (let [ns-sym (symbol target-str)]
        (require ns-sym)
        {:test-type :namespace :id ns-sym :display-name (str "Clojure Namespace: " target-str)})
      (catch Throwable e
        (println (str "[TestFactory] Error loading namespace '" target-str "': " (.getMessage e)))
        (.printStackTrace e)
        nil))))

(defn execute-test
  [type id display-name]

  (println "1")
  (let [current-counters (atom t/*initial-report-counters*)]
    (println "2")
    (binding [t/*report-counters* current-counters]
      (try

        (cond
          (= type :namespace) (t/run-tests id)
          (= type :var) (t/test-var id)
          :else (println (format "[%s] Unknown test type in executable:" id) type))

        (let [summary @current-counters]
          (println (format "[%s] Summary for %s: %s" id, display-name, summary))
          (when (or (pos? (:fail summary)) (pos? (:error summary)))
            (throw (AssertionFailedError.
                    (str display-name " failed. Fails: " (:fail summary) ", Errors: " (:error summary)
                         ". Check console for clojure.test output.")))))
        (catch Exception e
          (println (format "[%s] Exception during execution of %s" id display-name))
          (.printStackTrace e)
          (throw (AssertionFailedError.
                  (str display-name " execution error: " (.getMessage e)) e)))))))

(defn -createClojureTests
  [_this]
  (let [targets-str (System/getProperty "clojure.tests.targets" "")
        _ (println (str "[TestFactory] Received clojure.tests.targets: '" targets-str "'"))
        target-ids (->> (str/split targets-str #",")
                        (map str/trim)
                        (remove str/blank?)
                        (map parse-and-prepare-target)
                        (remove nil?)
                        vec)]

    (if (empty? target-ids)
      (println "[TestFactory] No valid Clojure test targets found to generate JUnit tests.")
      (println (str "[TestFactory] Generating " (count target-ids) " JUnit DynamicTest(s)...")))

    (let [target-ids
          (mapv (fn [{:keys [test-type id display-name]}]
                  (println (str "[" id "] Praparint test for: " test-type ", with display name: " display-name))
                  (let [executable (reify Executable
                                     (execute [_]
                                       (println (format "[%s] Executing: %s" id, display-name))
                                       (execute-test test-type id display-name)))]
                    (DynamicTest/dynamicTest display-name executable)))
                target-ids)]
      (println "[Testfactory] Create in total" (count target-ids) "tests")
      (into [] target-ids))))
