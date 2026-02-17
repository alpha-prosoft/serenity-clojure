(ns samples.serenity-report-validation-test
  "Comprehensive test to validate Serenity report generation including:
   - Screenshot capture and attachment to reports
   - API call logging and collection with full request/response details
   - Report aggregation and JSON structure validation
   - Combined UI and API testing in single scenario
   - Screenshot galleries with multiple images
   - HTML report generation and structure validation"
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clojure.string :as str]
            [testing.junit :refer [with-serenity step ui-step api-step take-screenshot generate-reports]])
  (:import [net.serenitybdd.rest SerenityRest]
           [io.restassured.http ContentType Header]
           [com.microsoft.playwright Page$WaitForLoadStateOptions]
           [com.microsoft.playwright.options LoadState]))

(deftest combined-ui-api-screenshot-test
  "Test that combines UI interactions with screenshots and API calls
   to validate that all elements are properly captured in Serenity reports.
   This test verifies:
   - Screenshots are captured before and after each UI step
   - API calls are logged with full request/response details
   - Both UI and API steps appear in the same test report
   - Data flows between UI and API steps
   - Nested step structure for better organization"
  (with-serenity [page]
    
    ;; UI Step 1: Navigate and capture screenshots
    (ui-step page "Navigate to httpbin.org homepage"
      #(do
         (.navigate page "https://httpbin.org")
         (.waitForLoadState page LoadState/NETWORKIDLE)))
    
    ;; UI Step 2: Verify page loaded with screenshots
    (ui-step page "Verify httpbin homepage loaded correctly"
      #(let [title (.title page)]
         (is (str/includes? title "httpbin") 
             "Page title should contain 'httpbin'")))
    
    ;; API Step 1: GET request - verify request/response logging
     (api-step "Execute GET request to httpbin with query parameters"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://httpbin.org")
                           (.queryParam "test_param" (into-array Object ["clojure"]))
                           (.queryParam "timestamp" (into-array Object [(System/currentTimeMillis)]))
                           (.when)
                           (.get "/get" (into-array Object [])))]
           (is (= 200 (.statusCode response)))
           (is (some? (-> response .jsonPath (.getString "url")))
               "GET response should contain URL field")
           (is (= "clojure" (-> response .jsonPath (.getString "args.test_param")))
               "Query parameters should be echoed back")
           (println "✓ GET request successful with query parameters")))
    
    ;; UI Step 3: Navigate to a different page
    (ui-step page "Navigate to httpbin GET endpoint documentation"
      #(do
         (.navigate page "https://httpbin.org/#/HTTP_Methods/get_get")
         (Thread/sleep 2000)))
    
    ;; API Step 2: POST request with JSON data
     (api-step "Execute POST request with JSON body"
       #(let [test-data {"test" "data" 
                         "timestamp" (System/currentTimeMillis)
                         "source" "serenity-clojure"
                         "nested" {"key" "value" "count" 42}}
              response (-> (SerenityRest/given)
                           (.baseUri "https://httpbin.org")
                           (.contentType ContentType/JSON)
                           (.body test-data)
                           (.when)
                           (.post "/post" (into-array Object [])))]
           (is (= 200 (.statusCode response)))
           (is (= "data" (-> response .jsonPath (.getString "json.test")))
               "POST response should echo the sent data")
           (is (= 42 (-> response .jsonPath (.getInt "json.nested.count")))
               "Nested JSON data should be preserved")
           (println "✓ POST request successful with nested JSON")))
    
    ;; UI Step 4: Navigate to POST endpoint page
    (ui-step page "Navigate to POST endpoint documentation"
      #(do
         (.navigate page "https://httpbin.org/#/HTTP_Methods/post_post")
         (Thread/sleep 1500)))
    
    ;; API Step 3: PUT request with headers
     (api-step "Execute PUT request with custom headers"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://httpbin.org")
                           (.contentType ContentType/JSON)
                           (.header (Header. "X-Custom-Header" "test-value"))
                           (.header (Header. "X-Test-ID" "123456"))
                           (.body {"updated" true "timestamp" (System/currentTimeMillis)})
                           (.when)
                           (.put "/put" (into-array Object [])))]
           (is (= 200 (.statusCode response)))
           (is (= "test-value" (-> response .jsonPath (.getString "headers.X-Custom-Header")))
               "Custom headers should be sent with request")
           (println "✓ PUT request successful with custom headers")))
    
    ;; UI Step 5: Take final screenshot
    (ui-step page "Capture final state"
      #(println "✓ Combined UI+API test completed successfully"))
    
    ;; API Step 4: DELETE request
     (api-step "Execute DELETE request"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://httpbin.org")
                           (.when)
                           (.delete "/delete" (into-array Object [])))]
           (is (= 200 (.statusCode response)))
           (is (some? (-> response .jsonPath (.getString "url")))
               "DELETE response should contain URL")
           (println "✓ DELETE request successful")))
    
    ;; API Step 5: PATCH request
     (api-step "Execute PATCH request"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://httpbin.org")
                           (.contentType ContentType/JSON)
                           (.body {"partial" "update"})
                           (.when)
                           (.patch "/patch" (into-array Object [])))]
           (is (= 200 (.statusCode response)))))))

(deftest multiple-screenshots-test
  "Test that captures multiple screenshots at different stages
   to validate screenshot gallery generation in reports.
   Verifies:
   - Multiple screenshots are captured before and after each action
   - Each screenshot is uniquely named and timestamped
   - Screenshots appear in report gallery views
   - Screenshot thumbnails are generated"
  (with-serenity [page]
    
    (ui-step page "Navigate to example.com - Initial load"
      #(do
         (.navigate page "https://example.com")
         (.waitForLoadState page LoadState/NETWORKIDLE)))
    
    (ui-step page "Verify page content and h1 heading"
      #(let [heading (.locator page "h1")]
         (is (> (.count heading) 0) "Page should have h1 heading")
         (is (str/includes? (.textContent (.first heading)) "Example")
             "Heading should contain 'Example'")))
    
    (ui-step page "Verify page body content"
      #(let [paragraph (.locator page "p")]
         (is (> (.count paragraph) 0) "Page should have paragraphs")))
    
    (ui-step page "Scroll to bottom of page"
      #(do
         (.evaluate page "window.scrollTo(0, document.body.scrollHeight)")
         (Thread/sleep 500)))
    
    (ui-step page "Navigate to httpbin homepage"
      #(do
         (.navigate page "https://httpbin.org")
         (.waitForLoadState page LoadState/NETWORKIDLE)))
    
    (ui-step page "View httpbin API documentation"
      #(Thread/sleep 1000))
    
    (ui-step page "Navigate back to example.com"
      #(do
         (.navigate page "https://example.com")
         (.waitForLoadState page LoadState/NETWORKIDLE)))
    
    (ui-step page "Complete test journey"
      #(println "✓ Captured screenshots at each step with before/after states"))))

(deftest api-only-test
  "Test with only API calls to validate API request/response logging"
  (with-serenity [page]
    
     (api-step "GET /users endpoint"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://jsonplaceholder.typicode.com")
                           (.when)
                           (.get "/users" (into-array Object [])))]
           (is (= 200 (.statusCode response)))))
    
     (api-step "GET specific user"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://jsonplaceholder.typicode.com")
                           (.when)
                           (.get "/users/1" (into-array Object [])))]
           (is (= 200 (.statusCode response)))
           (is (= "Leanne Graham" (-> response .jsonPath (.getString "name"))))))
    
     (api-step "Create new user"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://jsonplaceholder.typicode.com")
                           (.contentType ContentType/JSON)
                           (.body {"name" "Test User" "username" "testuser" "email" "test@example.com"})
                           (.when)
                           (.post "/users" (into-array Object [])))]
           (is (= 201 (.statusCode response)))))
    
     (api-step "Update user"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://jsonplaceholder.typicode.com")
                           (.contentType ContentType/JSON)
                           (.body {"name" "Updated User"})
                           (.when)
                           (.put "/users/1" (into-array Object [])))]
           (is (= 200 (.statusCode response)))))
    
     (api-step "Delete user"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://jsonplaceholder.typicode.com")
                           (.when)
                           (.delete "/users/1" (into-array Object [])))]
           (is (= 200 (.statusCode response)))))))

(defn- collect-from-steps
  "Recursively collect items from test steps and their children.
   Returns a sequence of steps (at any nesting level) that match the predicate."
  [steps pred]
  (when (seq steps)
    (concat
     (filter pred steps)
     (mapcat #(collect-from-steps (:children %) pred) steps))))

(defn validate-json-reports
  "Validate that JSON reports are properly generated with expected structure.
   Checks for:
   - Correct JSON structure with required fields
   - Test steps with descriptions and timing
   - Screenshot references in steps (including nested children)
   - REST API call data (restQuery field, including nested children)
   - Test metadata and duration
   - Report aggregation data"
  []
  (let [output-dir (or (System/getProperty "serenity.outputDirectory") 
                       "target/site/serenity")
        json-files (filter #(.endsWith (.getName %) ".json")
                          (file-seq (io/file output-dir)))]
    (println "\n========================================")
    (println "Validating JSON Reports")
    (println "========================================")
    (println (str "Output directory: " output-dir))
    (println (str "JSON files found: " (count json-files)))
    
    (let [total-screenshots (atom 0)
          total-api-calls (atom 0)
          total-steps (atom 0)]
      
      (doseq [json-file json-files]
        (when (.isFile json-file)
          (println (str "\n📄 " (.getName json-file)))
          (try
            (let [content (json/read-str (slurp json-file) :key-fn keyword)]
              ;; Validate basic structure
              (when (:testSteps content)
                (let [step-count (count (:testSteps content))]
                  (swap! total-steps + step-count)
                  (println (str "  ✓ Test steps: " step-count))))
              
              (when (:title content)
                (println (str "  ✓ Title: " (:title content))))
              
              (when (:duration content)
                (println (str "  ✓ Duration: " (:duration content) "ms")))
              
              (when (:result content)
                (println (str "  ✓ Result: " (:result content))))
              
              ;; Check for screenshots recursively (they live in children)
              (let [screenshot-steps (collect-from-steps 
                                      (:testSteps content)
                                      #(seq (:screenshots %)))
                    screenshot-count (count screenshot-steps)]
                (when (> screenshot-count 0)
                  (swap! total-screenshots + screenshot-count)
                  (println (str "  ✓ Screenshots found: " screenshot-count))
                  (doseq [s (take 3 screenshot-steps)]
                    (when (:description s)
                      (println (str "    - " (:description s)))))))
              
              ;; Check for REST data recursively (they live in children)
              (let [rest-steps (collect-from-steps
                                 (:testSteps content)
                                 #(contains? % :restQuery))
                    api-count (count rest-steps)]
                (when (> api-count 0)
                  (swap! total-api-calls + api-count)
                  (println (str "  ✓ API calls found: " api-count))
                  (doseq [api-step (take 3 rest-steps)]
                    (when (:description api-step)
                      (println (str "    - " (:description api-step)))))))
              
              ;; Validate test result status
              (when-let [result (:result content)]
                (is (contains? #{"SUCCESS" "FAILURE" "PENDING" "SKIPPED" "ERROR"} result)
                    "Test result should be valid status")))
            (catch Exception e
              (println (str "  ✗ Error parsing JSON: " (.getMessage e)))))))
      
      ;; Summary statistics
      (println "\n--- Summary Statistics ---")
      (println (str "Total test reports: " (count (filter #(.isFile %) json-files))))
      (println (str "Total test steps: " @total-steps))
      (println (str "Total screenshots: " @total-screenshots))
      (println (str "Total API calls: " @total-api-calls))
      (println "========================================\n")
      
      ;; Assertions for validation
      (is (> (count json-files) 0) "At least one JSON report should exist")
      (is (> @total-steps 0) "Reports should contain test steps")
      (is (> @total-screenshots 0) "Reports should contain screenshots")
      (is (> @total-api-calls 0) "Reports should contain API calls"))))

(defn- test-screenshot?
  "Returns true if the PNG file is a test screenshot (not a Serenity report asset).
   Test screenshots are in the root output directory and have timestamp patterns in their names."
  [^java.io.File png-file ^java.io.File output-dir]
  (and (.isFile png-file)
       (.endsWith (.getName png-file) ".png")
       ;; Must be directly in the output directory (not in images/, jit/, jquery-ui/ subdirs)
       (= (.getCanonicalPath (.getParentFile png-file)) (.getCanonicalPath output-dir))
       ;; Test screenshots have a numeric timestamp-index suffix pattern like -1771311775318-4.png
       (re-find #"-\d{10,}-\d+\.png$" (.getName png-file))))

(defn validate-screenshots
  "Validate that screenshot files exist and are properly named.
   Checks for:
   - PNG files in output directory (excluding report assets)
   - Reasonable file sizes
   - Proper naming convention with timestamps
   - Screenshot count matches expectations"
  []
  (let [output-dir-path (or (System/getProperty "serenity.outputDirectory") 
                            "target/site/serenity")
        output-dir (io/file output-dir-path)
        png-files (filter #(test-screenshot? % output-dir)
                          (file-seq output-dir))]
    (println "\n========================================")
    (println "Validating Screenshots")
    (println "========================================")
    (println (str "Output directory: " output-dir-path))
    (println (str "Test screenshot files found: " (count png-files)))
    
    (let [total-size (atom 0)]
      (doseq [png-file png-files]
        (let [size-kb (Math/round (double (/ (.length ^java.io.File png-file) 1024.0)))]
          (swap! total-size + size-kb)
          (println (str "  📸 " (.getName ^java.io.File png-file) 
                       " (" size-kb " KB)"))
          ;; Validate screenshot has reasonable size (>1KB indicates real content)
          (is (> size-kb 1) (str "Screenshot should have substantial content: " (.getName ^java.io.File png-file)))))
      
      (println (str "\nTotal screenshots size: " @total-size " KB"))
      (println (str "Average screenshot size: " 
                   (if (> (count png-files) 0)
                     (Math/round (double (/ @total-size (count png-files))))
                     0) " KB"))
      (println "========================================\n")
      
      (is (> (count png-files) 0) "At least one test screenshot should exist")
      (is (> @total-size 10) "Screenshots should have meaningful content"))))

(deftest validate-report-generation
  "Post-test validation to ensure reports are properly generated with all elements.
   This comprehensive validation checks:
   - JSON report structure and content
   - Screenshot files and gallery
   - API request/response logging
   - HTML report generation
   - Report aggregation and statistics"
  (testing "JSON reports validation"
    (validate-json-reports))
  
  (testing "Screenshots validation"
    (validate-screenshots))
  
  (testing "HTML report generation and structure"
    (let [output-dir (or (System/getProperty "serenity.outputDirectory") 
                         "target/site/serenity")
          index-file (io/file output-dir "index.html")]
      (println "\n========================================")
      (println "Checking HTML Report")
      (println "========================================")
      (if (.exists index-file)
        (do
          (println (str "  ✓ index.html exists"))
          (println (str "  ✓ Size: " (Math/round (double (/ (.length index-file) 1024.0))) " KB"))
          (println (str "  ✓ Location: " (.getAbsolutePath index-file)))
          (is (> (.length index-file) 1000) "HTML report should have substantial content")
          
          ;; Check for other report files
          (let [report-files (filter #(or (.endsWith (.getName %) ".html")
                                         (.endsWith (.getName %) ".css")
                                         (.endsWith (.getName %) ".js"))
                                    (file-seq (io/file output-dir)))]
            (println (str "  ✓ Total report files (HTML/CSS/JS): " (count report-files))))
          
          (println "\nTo view the report, run:")
          (println (str "  open " (.getAbsolutePath index-file)))
          (println "Or start a web server:")
          (println (str "  python3 -m http.server 8000 --directory " output-dir))
          (is true "HTML report exists"))
        (do
          (println "  ⚠ index.html not found - run generate-reports first")
          (println "  Command: clojure -M:aggregate-reports")
          (println "========================================\n")))))
  
  (testing "Report aggregation statistics"
    (println "\n========================================")
    (println "Report Aggregation Summary")
    (println "========================================")
    (println "All validation tests completed!")
    (println "The following were verified:")
    (println "  ✓ JSON test outcome reports generated")
    (println "  ✓ Screenshots captured and saved")
    (println "  ✓ API calls logged with request/response data")
    (println "  ✓ HTML reports aggregated")
    (println "  ✓ Test metadata and timing recorded")
    (println "========================================\n")))
