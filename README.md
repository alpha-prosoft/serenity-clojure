# Serenity Clojure

[![Clojure](https://img.shields.io/badge/Clojure-1.12.4-blue.svg)](https://clojure.org/)
[![Serenity BDD](https://img.shields.io/badge/Serenity%20BDD-5.0.2-green.svg)](https://serenity-bdd.info/)
[![Playwright](https://img.shields.io/badge/Playwright-1.57.0-orange.svg)](https://playwright.dev/)
[![Clojars Project](https://img.shields.io/clojars/v/com.alpha-prosoft/serenity-clojure.svg)](https://clojars.org/com.alpha-prosoft/serenity-clojure)

A powerful, elegant testing library for Clojure that combines **Serenity BDD** reporting with **Playwright** browser automation and **REST Assured** API testing. Write beautiful, readable tests in pure Clojure with automatic screenshots, step-by-step reporting, and rich HTML documentation.

## Features

- **Pure Clojure API**: Write tests using standard `clojure.test` - no Java interop required
- **Automatic Before/After Screenshots**: UI steps capture screenshots before and after each action
- **Step-Based Reporting**: Every test step is tracked with timing, status, and full context
- **Browser Automation**: Full Playwright integration for modern web testing
- **API Testing**: REST Assured integration with automatic request/response logging
- **Rich HTML Reports**: Beautiful, detailed reports with screenshots, timelines, and analytics
- **Zero Configuration**: Works out of the box with sensible defaults

## Quick Start

### Installation

Add to your `deps.edn`:

```clojure
{:deps {com.alpha-prosoft/serenity-clojure {:mvn/version "1.4"}}}
```

Or with Leiningen (`project.clj`):

```clojure
:dependencies [[com.alpha-prosoft/serenity-clojure "1.4"]]
```

**Install Playwright browsers:**
```bash
npx playwright install chromium
```

### Your First Test

```clojure
(ns my-app.tests
  (:require [clojure.test :refer [deftest is]]
            [testing.junit :refer [with-serenity ui-step]]))

(deftest simple-navigation-test
  (with-serenity [page]
    
    (ui-step page "Navigate to example.com"
      #(.navigate page "https://example.com"))
    
    (ui-step page "Verify page title"
      #(is (clojure.string/includes? (.title page) "Example")))))
```

**Generate reports:**
```bash
clojure -M -e "(require '[testing.junit :as junit])(junit/generate-reports)"
```

**View results:**
Open `target/site/serenity/index.html` in your browser to see beautiful test reports with embedded screenshots.

## API Reference

### Core Macros & Functions

#### `with-serenity [page]`

Main test wrapper that provides:
- Automatic Playwright browser lifecycle management
- Serenity BDD event bus integration
- JSON report generation
- Automatic cleanup on test completion

```clojure
(deftest my-test
  (with-serenity [page]
    ;; `page` is a Playwright Page instance
    ;; Browser runs in headless Chromium by default
    ;; All events are automatically reported to Serenity
    (.navigate page "https://example.com")))
```

#### `step [description f]`

Execute a test step with automatic reporting:
- Logs step start and completion
- Reports duration in milliseconds
- Captures failures with full stack traces
- Integrates with StepEventBus for report generation

```clojure
(step "Navigate to homepage"
  #(.navigate page "https://example.com"))
```

#### `ui-step [page description f]`

Execute a UI step with **automatic before/after screenshots**:
- Creates nested "Before" and "After" child steps
- Automatically captures screenshot before the action
- Executes the UI action
- Automatically captures screenshot after the action
- Both screenshots appear in reports under the parent step

```clojure
(ui-step page "Login to application"
  #(do
     (.fill page "#username" "testuser")
     (.fill page "#password" "password")
     (.click page "#login-button")))
```

This creates a step hierarchy:
```
Login to application
├── Login to application - Before (screenshot)
└── Login to application - After (screenshot)
```

**Perfect for:**
- Documenting UI state changes
- Visual regression testing
- Before/after comparisons
- Step-by-step UI journeys

#### `api-step [description f]`

Execute an API call with automatic REST logging:
- Wraps `step` with SerenityRest integration
- Automatically captures HTTP request/response details
- Resets REST state after each call
- Includes full request/response data in reports

```clojure
(api-step "Get user details"
  #(-> (SerenityRest/given)
       (.get "/users/1")
       (.then)
       (.statusCode 200)))
```

#### `take-screenshot [page description]`

Capture and attach screenshot to current step:
- Automatically saves PNG to output directory
- Embeds screenshot in HTML reports
- Creates clickable thumbnails in step views
- Generates screenshot gallery pages
- Returns filename for verification

```clojure
(take-screenshot page "after-login")
;; => "after_login-1767163337862-1.png"
```

Screenshots appear in reports as:
- Inline thumbnails in step lists
- Full-size images in dedicated gallery pages
- Clickable previews with timestamps

### Utility Functions

#### `generate-reports []`

Generate HTML reports from JSON test results:
```clojure
(require '[testing.junit :as junit])
(junit/generate-reports)
```

Outputs:
```
========================================
Generating Serenity Reports
========================================
  Source: target/site/serenity
  Project: Serenity Clojure
  ✓ HTML reports generated
  Open: target/site/serenity/index.html
========================================
```

## Configuration

### `serenity.properties`
```properties
# Project identification
serenity.project.name=My Test Project

# Output directory
serenity.outputDirectory=target/site/serenity

# Screenshot settings (manual via take-screenshot)
serenity.take.screenshots=FOR_EACH_ACTION

# Report configuration
serenity.report.json=true
serenity.report.show.step.details=true
serenity.console.colors=true
```

## Running Tests

### Run all tests
```bash
clojure -M:test -e "(require 'clojure.test)(clojure.test/run-all-tests #\"samples.*\")"
```

### Run specific namespace
```bash
clojure -M:test -e "(require '[samples.demo-test])(clojure.test/run-tests 'samples.demo-test)"
```

### Run with Kaocha (recommended)
```bash
clojure -M:it
```

### Generate reports
```bash
clojure -M:aggregate-reports
```

Or manually:
```bash
clojure -M -e "(require '[testing.junit :as junit])(junit/generate-reports)"
```

### View reports
```bash
# Open in browser (macOS)
open target/site/serenity/index.html

# Linux
xdg-open target/site/serenity/index.html

# Or use Python web server
python3 -m http.server 8000 --directory target/site/serenity
# Then visit http://localhost:8000
```

## Advanced Examples

### UI Testing with Automatic Before/After Screenshots

The `ui-step` macro automatically captures screenshots before and after each UI action, creating a visual timeline of your test:

```clojure
(ns my-app.ui-test
  (:require [clojure.test :refer [deftest is]]
            [testing.junit :refer [with-serenity ui-step api-step]])
  (:import [com.microsoft.playwright.options LoadState]))

(deftest user-login-journey
  (with-serenity [page]
    
    ;; Each ui-step creates Before and After screenshots automatically
    (ui-step page "Navigate to login page"
      #(do
         (.navigate page "https://myapp.com/login")
         (.waitForLoadState page LoadState/NETWORKIDLE)))
    
    (ui-step page "Fill in login credentials"
      #(do
         (.fill page "#username" "testuser")
         (.fill page "#password" "password123")))
    
    (ui-step page "Submit login form"
      #(.click page "button[type='submit']"))
    
    (ui-step page "Verify successful login"
      #(do
         (.waitForSelector page ".dashboard")
         (is (.isVisible (.locator page ".user-profile")))))))
```

**Report Output:**
```
✓ Navigate to login page
  ├── Navigate to login page - Before (screenshot)
  └── Navigate to login page - After (screenshot)
✓ Fill in login credentials
  ├── Fill in login credentials - Before (screenshot)
  └── Fill in login credentials - After (screenshot)
✓ Submit login form
  ├── Submit login form - Before (screenshot)
  └── Submit login form - After (screenshot)
✓ Verify successful login
  ├── Verify successful login - Before (screenshot)
  └── Verify successful login - After (screenshot)
```

This creates **8 screenshots** (4 steps × 2 screenshots each) documenting every state change in your UI test.

### Complete Test Workflow - From Writing to Validation

Here's a complete workflow that demonstrates how to write comprehensive tests, generate reports, and validate that all artifacts are properly created:

```bash
# 1. Clean previous test outputs
clojure -M:clean

# 2. Run your tests
clojure -M:test -e "(require '[samples.serenity-report-validation-test])(clojure.test/run-tests 'samples.serenity-report-validation-test)"

# 3. Generate HTML reports
clojure -M:aggregate-reports

# 4. View reports
open target/site/serenity/index.html
```

### Combined UI and API Testing

Test that combines browser automation with API calls in a single scenario, using automatic before/after screenshots for UI steps:

```clojure
(ns my-app.integration-test
  (:require [clojure.test :refer [deftest is]]
            [testing.junit :refer [with-serenity ui-step api-step]])
  (:import [net.serenitybdd.rest SerenityRest]
           [io.restassured.http ContentType]
           [com.microsoft.playwright.options LoadState]))

(deftest combined-ui-api-test
  (with-serenity [page]
    
    ;; UI Testing with automatic before/after screenshots
    (ui-step page "Navigate to application homepage"
      #(do
         (.navigate page "https://example.com")
         (.waitForLoadState page LoadState/NETWORKIDLE)))
    
    (ui-step page "Verify page loaded correctly"
      #(is (clojure.string/includes? (.title page) "Example")))
    
    ;; API Testing - Call REST endpoints with full logging
    (api-step "Fetch data from API with query parameters"
      #(let [response (-> (SerenityRest/given)
                          (.baseUri "https://api.example.com")
                          (.queryParam "userId" "123")
                          (.when)
                          (.get "/data")
                          (.then)
                          (.statusCode 200)
                          (.extract)
                          (.response))]
         (is (some? (-> response .jsonPath (.getString "id"))))))
    
    ;; More API testing with POST request
    (api-step "Create new resource via API"
      #(-> (SerenityRest/given)
           (.baseUri "https://api.example.com")
           (.contentType ContentType/JSON)
           (.body {"name" "Test" "value" 42})
           (.when)
           (.post "/resources")
           (.then)
           (.statusCode 201)))
    
    ;; Continue UI interaction with automatic screenshots
    (ui-step page "Navigate to details page"
      #(.navigate page "https://example.com/details"))))
```

This test will generate a Serenity report that includes:
- **4 UI screenshots** (2 UI steps × 2 screenshots each = before/after for each step)
- Full API request/response details (URL, headers, body, status)
- Step-by-step execution timeline
- Combined test duration and statistics

### Testing with Multiple Screenshots - Gallery Validation

Create comprehensive screenshot galleries using `ui-step` for automatic before/after documentation, or use manual screenshots with the `step` macro for custom scenarios:

```clojure
(ns my-app.visual-test
  (:require [clojure.test :refer [deftest]]
            [testing.junit :refer [with-serenity ui-step]]))

(deftest visual-journey-test
  (with-serenity [page]
    
    ;; Each ui-step automatically creates before/after screenshots
    (ui-step page "Navigate to homepage"
      #(.navigate page "https://example.com"))
    
    (ui-step page "Fill in login form"
      #(do
         (.fill page "#username" "testuser")
         (.fill page "#password" "password")))
    
    (ui-step page "Submit and login"
      #(.click page "#login-button"))
    
    (ui-step page "Open user dashboard"
      #(.click page "a:has-text(\"Dashboard\")"))
    
    (ui-step page "Navigate to profile section"
      #(.click page "a:has-text(\"Profile\")"))
    
    (ui-step page "View settings page"
      #(.click page "a:has-text(\"Settings\")"))
    
    (ui-step page "Logout from application"
      #(.click page "#logout"))))
```

**Result**: This test creates **14 screenshots** (7 UI steps × 2 screenshots each) showing the complete user journey with before/after states for every interaction. Serenity generates a screenshot gallery with all images clickable and organized by test step in the HTML report.

### API-Only Testing with Full Request/Response Logging

Focus on REST API testing with automatic request/response capture:

```clojure
(deftest comprehensive-api-test
  (with-serenity [page]
    
    (api-step "GET request with headers and query params"
      #(let [response (-> (SerenityRest/given)
                          (.baseUri "https://api.example.com")
                          (.header "X-API-Key" "test-key")
                          (.queryParam "filter" "active")
                          (.queryParam "limit" 10)
                          (.when)
                          (.get "/users")
                          (.then)
                          (.statusCode 200)
                          (.extract)
                          (.response))]
         (is (> (count (-> response .jsonPath (.getList "data"))) 0))))
    
    (api-step "POST request with JSON body and custom headers"
      #(let [data {"name" "New User" 
                   "email" "test@example.com"
                   "metadata" {"source" "test" "version" "1.0"}}
             response (-> (SerenityRest/given)
                          (.baseUri "https://api.example.com")
                          (.contentType ContentType/JSON)
                          (.header "X-Request-ID" "req-123")
                          (.body data)
                          (.when)
                          (.post "/users")
                          (.then)
                          (.statusCode 201)
                          (.extract)
                          (.response))]
         (is (= "New User" (-> response .jsonPath (.getString "name"))))))
    
    (api-step "PUT request to update resource"
      #(-> (SerenityRest/given)
           (.baseUri "https://api.example.com")
           (.contentType ContentType/JSON)
           (.body {"status" "updated"})
           (.when)
           (.put "/users/1")
           (.then)
           (.statusCode 200)))
    
    (api-step "DELETE request"
      #(-> (SerenityRest/given)
           (.baseUri "https://api.example.com")
           (.when)
           (.delete "/users/1")
           (.then)
           (.statusCode 204)))))
```

**API Logging includes**:
- Request URL, method, headers
- Request body (formatted JSON)
- Response status, headers
- Response body (formatted JSON)
- Request/response timing

### Report Validation - Ensuring Proper Generation

Test that combines browser automation with API calls in a single scenario:

```clojure
(ns my-app.integration-test
  (:require [clojure.test :refer [deftest is]]
            [testing.junit :refer [with-serenity step api-step take-screenshot]])
  (:import [net.serenitybdd.rest SerenityRest]
           [io.restassured.http ContentType]
           [com.microsoft.playwright Page$WaitForLoadStateOptions LoadState]))

(deftest combined-ui-api-test
  (with-serenity [page]
    
    ;; UI Testing
    (step "Navigate to application homepage"
      #(do
         (.navigate page "https://example.com")
         (.waitForLoadState page LoadState/NETWORKIDLE)
         (take-screenshot page "homepage")))
    
    (step "Verify page loaded"
      #(do
         (is (clojure.string/includes? (.title page) "Example"))
         (take-screenshot page "verified")))
    
    ;; API Testing
    (api-step "Fetch data from API"
      #(let [response (-> (SerenityRest/given)
                          (.baseUri "https://api.example.com")
                          (.when)
                          (.get "/data")
                          (.then)
                          (.statusCode 200)
                          (.extract)
                          (.response))]
         (is (some? (-> response .jsonPath (.getString "id"))))))
    
    ;; More UI interaction based on API data
    (step "Navigate to details page"
      #(do
         (.navigate page "https://example.com/details")
         (take-screenshot page "details-page")))))
```

### API-Only Testing

Focus on REST API testing with full request/response logging:

```clojure
(deftest api-crud-test
  (with-serenity [page]
    
    (api-step "Create new resource"
      #(let [data {"name" "Test" "value" 123}
             response (-> (SerenityRest/given)
                          (.baseUri "https://api.example.com")
                          (.contentType ContentType/JSON)
                          (.body data)
                          (.when)
                          (.post "/resources")
                          (.then)
                          (.statusCode 201)
                          (.extract)
                          (.response))]
         (is (some? (-> response .jsonPath (.getInt "id"))))))
    
    (api-step "Read resource"
      #(-> (SerenityRest/given)
           (.baseUri "https://api.example.com")
           (.when)
           (.get "/resources/1")
           (.then)
           (.statusCode 200)))
    
    (api-step "Update resource"
      #(-> (SerenityRest/given)
           (.baseUri "https://api.example.com")
           (.contentType ContentType/JSON)
           (.body {"name" "Updated"})
           (.when)
           (.put "/resources/1")
           (.then)
           (.statusCode 200)))
    
    (api-step "Delete resource"
      #(-> (SerenityRest/given)
           (.baseUri "https://api.example.com")
           (.when)
           (.delete "/resources/1")
           (.then)
           (.statusCode 204)))))
```

### Multiple Screenshots and Gallery

Capture multiple screenshots to create a visual test journey:

```clojure
(deftest visual-journey-test
  (with-serenity [page]
    
    (step "Step 1: Homepage"
      #(do
         (.navigate page "https://example.com")
         (take-screenshot page "01-homepage")))
    
    (step "Step 2: Login"
      #(do
         (.fill page "#username" "testuser")
         (.fill page "#password" "password")
         (take-screenshot page "02-before-login")
         (.click page "#login-button")
         (take-screenshot page "03-after-login")))
    
    (step "Step 3: Navigate sections"
      #(do
         (.click page "a:has-text(\"Dashboard\")")
         (take-screenshot page "04-dashboard")
         (.click page "a:has-text(\"Profile\")")
         (take-screenshot page "05-profile")))
    
    (step "Step 4: Logout"
      #(do
         (.click page "#logout")
         (take-screenshot page "06-logged-out")))))
```

### Report Validation

Validate that Serenity reports are properly generated with all artifacts:

```clojure
(ns my-app.validation-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]
            [testing.junit :refer [generate-reports]]))

(defn validate-screenshots []
  "Check that screenshot files exist"
  (let [output-dir "target/site/serenity"
        png-files (filter #(.endsWith (.getName %) ".png")
                         (file-seq (io/file output-dir)))]
    (println (str "Screenshots found: " (count png-files)))
    (is (> (count png-files) 0) "Screenshots should be captured")))

(defn validate-json-reports []
  "Verify JSON test reports have correct structure"
  (let [output-dir "target/site/serenity"
        json-files (filter #(.endsWith (.getName %) ".json")
                          (file-seq (io/file output-dir)))]
    (doseq [json-file json-files]
      (when (.isFile json-file)
        (let [content (json/read-str (slurp json-file) :key-fn keyword)]
          (is (contains? content :testSteps) "Report should have test steps")
          (is (contains? content :title) "Report should have title")
          
          ;; Check for screenshots in steps
          (let [screenshots (filter #(contains? % :screenshot) 
                                   (:testSteps content))]
            (println (str "Screenshots in report: " (count screenshots))))
          
          ;; Check for API calls
          (let [api-calls (filter #(contains? % :restQuery) 
                                 (:testSteps content))]
            (println (str "API calls in report: " (count api-calls)))))))))

(defn validate-html-report []
  "Check that HTML report exists"
  (let [index-file (io/file "target/site/serenity/index.html")]
    (is (.exists index-file) "HTML report should be generated")
    (println (str "Report location: " (.getAbsolutePath index-file)))))

(deftest report-validation-test
  (testing "Validate report artifacts"
    (validate-screenshots)
    (validate-json-reports)
    (validate-html-report)))
```

### Complete Test Workflow

Here's a complete workflow from writing tests to viewing reports:

```bash
# 1. Clean previous test outputs
clojure -M:clean

# 2. Run your tests
clojure -M:it

# 3. Generate HTML reports
clojure -M:aggregate-reports

# 4. View reports
open target/site/serenity/index.html
```

### What Gets Captured in Reports

**Serenity reports automatically include:**

1. **Test Steps**: Every `step`, `ui-step`, and `api-step` call with timing and status
2. **Nested Screenshots**: For `ui-step`, automatic before/after screenshots as nested child steps
3. **Manual Screenshots**: Images captured via `take-screenshot` with thumbnails and galleries
4. **API Calls**: Full HTTP request/response details including:
   - Request URL, method, headers, body
   - Response status, headers, body
   - Timing information
5. **Failures**: Stack traces and error messages
6. **Test Metadata**: Duration, timestamps, test structure
7. **Aggregates**: Summary statistics across all tests

**Step Hierarchy Example:**
```
✓ Login to application (parent step)
  ├── Login to application - Before (child step with screenshot)
  └── Login to application - After (child step with screenshot)
✓ API: Get user data (api step with full request/response)
```

**Report Structure:**
```
target/site/serenity/
├── index.html              # Main dashboard
├── *.json                  # Test result data
├── *.png                   # Screenshots (before/after pairs)
├── serenity.css           # Styling
└── serenity.js            # Interactive features
```

## Design Philosophy

**Serenity Clojure** follows these principles:

1. **Clojure-First**: Pure Clojure API, Java interop hidden in macros
2. **Batteries Included**: Everything needed for modern web testing
3. **Beautiful Reports**: Screenshots and details by default
4. **Developer Experience**: Clear error messages, helpful output
5. **Zero Magic**: Explicit, understandable test structure

## License

See [LICENSE](LICENSE) file for details.

## Credits

Built with:
- [Serenity BDD](https://serenity-bdd.info/) - Beautiful test reporting
- [Playwright](https://playwright.dev/) - Modern browser automation
- [REST Assured](https://rest-assured.io/) - REST API testing
- [Clojure](https://clojure.org/) - The best language for testing

---

Made with ❤️ for the Clojure testing community
