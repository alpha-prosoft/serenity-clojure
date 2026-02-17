# Serenity Clojure

[![Clojure](https://img.shields.io/badge/Clojure-1.12.4-blue.svg)](https://clojure.org/)
[![Serenity BDD](https://img.shields.io/badge/Serenity%20BDD-5.2.2-green.svg)](https://serenity-bdd.info/)
[![Playwright](https://img.shields.io/badge/Playwright-1.58.0-orange.svg)](https://playwright.dev/)
[![Clojars Project](https://img.shields.io/clojars/v/com.alpha-prosoft/serenity-clojure.svg)](https://clojars.org/com.alpha-prosoft/serenity-clojure)

A powerful, elegant testing library for Clojure that combines **Serenity BDD** reporting with **Playwright** browser automation and **REST Assured** API testing. Write beautiful, readable tests in pure Clojure with automatic screenshots, step-by-step reporting, and rich HTML documentation.

## Features

- **Pure Clojure API**: Write tests using standard `clojure.test` - no Java interop required
- **Automatic Before/After Screenshots**: UI steps capture screenshots before and after each action
- **Step-Based Reporting**: Every test step is tracked with timing, status, and full context
- **Browser Automation**: Full Playwright integration for modern web testing
- **API Testing**: REST Assured integration via SerenityRest with automatic request/response logging
- **Rich HTML Reports**: Beautiful, detailed reports with screenshots, timelines, and analytics
- **Zero Configuration**: Works out of the box with sensible defaults

## Quick Start

### Interactive REPL (Browser Exploration)

Start an nREPL with a headed browser for interactive exploration:

```bash
clj -M:nrepl
```

Then in your editor (or any nREPL client connected to port 7888):

```clojure
(require '[testing.repl :refer [page restart stop]])

(.navigate @page "https://example.com")
(.textContent (.locator @page "h1"))   ;; => "Example Domain"
(.screenshot @page)

;; Use the same Playwright API as in tests
(.fill (.locator @page "#email") "test@example.com")
(.click (.locator @page "button"))

(restart)  ;; restart browser
(stop)     ;; close browser
```

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
- Enables relaxed HTTPS validation for test environments
- Resets REST state after each call
- Includes full request/response data in reports

**Important**: Use `SerenityRest/given` (not `RestAssured/given`) to ensure API calls are logged in Serenity reports. Capture the response directly from the HTTP method call (`.get`, `.post`, etc.) rather than using `.extract().response()`.

```clojure
(api-step "Get user details"
  #(let [response (-> (SerenityRest/given)
                      (.baseUri "https://jsonplaceholder.typicode.com")
                      (.when)
                      (.get "/users/1" (into-array Object [])))]
     (is (= 200 (.statusCode response)))
     (is (= "Leanne Graham" (-> response .jsonPath (.getString "name"))))))
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
Navigate to login page
├── Navigate to login page - Before (screenshot)
└── Navigate to login page - After (screenshot)
Fill in login credentials
├── Fill in login credentials - Before (screenshot)
└── Fill in login credentials - After (screenshot)
Submit login form
├── Submit login form - Before (screenshot)
└── Submit login form - After (screenshot)
Verify successful login
├── Verify successful login - Before (screenshot)
└── Verify successful login - After (screenshot)
```

This creates **8 screenshots** (4 steps x 2 screenshots each) documenting every state change in your UI test.

### Combined UI and API Testing

Test that combines browser automation with API calls in a single scenario, using automatic before/after screenshots for UI steps:

```clojure
(ns my-app.integration-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
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
      #(is (str/includes? (.title page) "Example")))
    
    ;; API Testing - Call REST endpoints with full logging
    (api-step "Fetch user data from API"
      #(let [response (-> (SerenityRest/given)
                          (.baseUri "https://jsonplaceholder.typicode.com")
                          (.when)
                          (.get "/users/1" (into-array Object [])))]
         (is (= 200 (.statusCode response)))
         (is (= "Leanne Graham" (-> response .jsonPath (.getString "name"))))))
    
    ;; More API testing with POST request
    (api-step "Create new resource via API"
      #(let [response (-> (SerenityRest/given)
                          (.baseUri "https://jsonplaceholder.typicode.com")
                          (.contentType ContentType/JSON)
                          (.body {"name" "Test" "value" 42})
                          (.when)
                          (.post "/posts" (into-array Object [])))]
         (is (= 201 (.statusCode response)))))
    
    ;; Continue UI interaction with automatic screenshots
    (ui-step page "Verify page content after API calls"
      #(let [heading (.locator page "h1")]
         (is (> (.count heading) 0))))))
```

This test will generate a Serenity report that includes:
- **6 UI screenshots** (3 UI steps x 2 screenshots each = before/after for each step)
- Full API request/response details (URL, headers, body, status)
- Step-by-step execution timeline
- Combined test duration and statistics

### API-Only Testing with Full Request/Response Logging

Focus on REST API testing with automatic request/response capture:

```clojure
(ns my-app.api-test
  (:require [clojure.test :refer [deftest is]]
            [testing.junit :refer [with-serenity api-step]])
  (:import [net.serenitybdd.rest SerenityRest]
           [io.restassured.http ContentType]))

(deftest comprehensive-api-test
  (with-serenity [page]
    
    (api-step "GET request with query parameters"
      #(let [response (-> (SerenityRest/given)
                          (.baseUri "https://jsonplaceholder.typicode.com")
                          (.queryParam "userId" (into-array Object ["1"]))
                          (.when)
                          (.get "/posts" (into-array Object [])))]
         (is (= 200 (.statusCode response)))))
    
    (api-step "POST request with JSON body"
      #(let [data {"title" "New Post" 
                   "body" "Post content"
                   "userId" 1}
             response (-> (SerenityRest/given)
                          (.baseUri "https://jsonplaceholder.typicode.com")
                          (.contentType ContentType/JSON)
                          (.body data)
                          (.when)
                          (.post "/posts" (into-array Object [])))]
         (is (= 201 (.statusCode response)))
         (is (= "New Post" (-> response .jsonPath (.getString "title"))))))
    
    (api-step "PUT request to update resource"
      #(let [response (-> (SerenityRest/given)
                          (.baseUri "https://jsonplaceholder.typicode.com")
                          (.contentType ContentType/JSON)
                          (.body {"title" "Updated Post"})
                          (.when)
                          (.put "/posts/1" (into-array Object [])))]
         (is (= 200 (.statusCode response)))))
    
    (api-step "DELETE request"
      #(let [response (-> (SerenityRest/given)
                          (.baseUri "https://jsonplaceholder.typicode.com")
                          (.when)
                          (.delete "/posts/1" (into-array Object [])))]
         (is (= 200 (.statusCode response)))))))
```

**API Logging includes**:
- Request URL, method, headers
- Request body (formatted JSON)
- Response status, headers
- Response body (formatted JSON)
- Request/response timing

### Report Validation

Validate that Serenity reports are properly generated with all artifacts. Note that screenshots and API call data are stored in nested `children` within test steps, not at the top level:

```clojure
(ns my-app.validation-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

(defn collect-from-steps
  "Recursively collect items from test steps and their children."
  [steps pred]
  (when (seq steps)
    (concat
     (filter pred steps)
     (mapcat #(collect-from-steps (:children %) pred) steps))))

(defn validate-json-reports []
  (let [output-dir "target/site/serenity"
        json-files (filter #(.endsWith (.getName %) ".json")
                          (file-seq (io/file output-dir)))]
    (doseq [json-file json-files]
      (when (.isFile json-file)
        (let [content (json/read-str (slurp json-file) :key-fn keyword)]
          (is (contains? content :testSteps) "Report should have test steps")
          
          ;; Screenshots are nested in children of test steps
          (let [screenshots (collect-from-steps
                              (:testSteps content)
                              #(seq (:screenshots %)))]
            (println (str "Screenshots in report: " (count screenshots))))
          
          ;; API calls (restQuery) are also nested in children
          (let [api-calls (collect-from-steps
                            (:testSteps content)
                            #(contains? % :restQuery))]
            (println (str "API calls in report: " (count api-calls)))))))))

(deftest report-validation-test
  (testing "Validate report artifacts"
    (validate-json-reports)))
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
Login to application (parent step)
├── Login to application - Before (child step with screenshot)
└── Login to application - After (child step with screenshot)
API: Get user data (api step with full request/response in child)
└── GET https://api.example.com/users/1 (child with restQuery data)
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

## Dependencies

| Library | Version | Purpose |
|---------|---------|---------|
| Clojure | 1.12.4 | Language runtime |
| Serenity BDD | 5.2.2 | Test reporting and BDD framework |
| Playwright | 1.58.0 | Browser automation |
| REST Assured | 6.0.0 | REST API testing (via serenity-rest-assured) |
| JUnit Platform | 6.0.3 | Test platform integration |
| Kaocha | 1.91.1392 | Clojure test runner |
| SLF4J | 2.0.17 | Logging facade |
| Logback | 1.5.32 | Logging implementation |

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

Made with care for the Clojure testing community
