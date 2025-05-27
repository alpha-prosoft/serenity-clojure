# Serenity Clojure

[![Clojure](https://img.shields.io/badge/Clojure-1.12.4-blue.svg)](https://clojure.org/)
[![Serenity BDD](https://img.shields.io/badge/Serenity%20BDD-5.0.2-green.svg)](https://serenity-bdd.info/)
[![Playwright](https://img.shields.io/badge/Playwright-1.57.0-orange.svg)](https://playwright.dev/)
[![Clojars Project](https://img.shields.io/clojars/v/com.alpha-prosoft/serenity-clojure.svg)](https://clojars.org/com.alpha-prosoft/serenity-clojure)

A powerful, elegant testing library for Clojure that combines **Serenity BDD** reporting with **Playwright** browser automation and **REST Assured** API testing. Write beautiful, readable tests in pure Clojure with automatic screenshots, step-by-step reporting, and rich HTML documentation.

## Features

- **Pure Clojure API**: Write tests using standard `clojure.test` - no Java interop required
- **Automatic Screenshot Capture**: Screenshots are embedded directly in HTML reports with gallery views
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
            [testing.junit :refer [with-serenity step take-screenshot]]))

(deftest simple-navigation-test
  (with-serenity [page]
    
    (step "Navigate to example.com"
      #(do
         (.navigate page "https://example.com")
         (take-screenshot page "homepage")))
    
    (step "Verify page title"
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

### Run specific namespace
```bash
clojure -M:test -e "(require '[my-app.tests])(clojure.test/run-tests 'my-app.tests)"
```

### Generate reports
```bash
clojure -M -e "(require '[testing.junit :as junit])(junit/generate-reports)"
```

### View reports
```bash
# Open in browser
open target/site/serenity/index.html

# Or use Python
python3 -m http.server 8000 --directory target/site/serenity
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
