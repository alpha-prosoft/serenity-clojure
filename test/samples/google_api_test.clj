(ns samples.google-api-test
  (:require [clojure.test :refer [deftest is]]
            [testing.junit :refer [with-serenity step api-step take-screenshot]])
  (:import [net.serenitybdd.rest SerenityRest]
           [io.restassured.http ContentType]
           [com.microsoft.playwright Page$WaitForLoadStateOptions LoadState]))

(deftest comprehensive-test
  (with-serenity [page]
    
    ;; UI Testing - Google Search
    (step "Navigate to Google"
      #(do
         (.navigate page "https://www.google.com")
         (.waitForLoadState page LoadState/NETWORKIDLE)
         (take-screenshot page "google-homepage")))
    
    (step "Handle cookie consent if present"
      #(try
         (let [consent-button (.locator page "button:has-text(\"Accept all\"), button:has-text(\"Reject all\")")]
           (when (> (.count consent-button) 0)
             (.click (.first consent-button))
             (Thread/sleep 1000)))
         (catch Exception e
           (println "No consent dialog or already handled"))))
    
    (step "Search for Serenity BDD"
      #(do
         (.fill page "textarea[name='q']" "Serenity BDD")
         (.press page "textarea[name='q']" "Enter")
         (.waitForLoadState page LoadState/NETWORKIDLE)
         (take-screenshot page "search-results")))
    
    (step "Verify search results"
      #(do
         (let [results (.locator page "h3")]
           (is (> (.count results) 0) "Search results should be present"))
         (take-screenshot page "results-verified")))
    
    ;; API Testing - JSONPlaceholder
    (api-step "Fetch user from JSONPlaceholder"
      #(let [response (-> (SerenityRest/given)
                          (.baseUri "https://jsonplaceholder.typicode.com")
                          (.when)
                          (.get "/users/1")
                          (.then)
                          (.statusCode 200)
                          (.extract)
                          (.response))]
         (is (= "Leanne Graham" (-> response .jsonPath (.getString "name"))))
         (is (= "Bret" (-> response .jsonPath (.getString "username"))))))
    
    (api-step "Create new post"
      #(let [post-data {"title" "Test Post"
                        "body" "This is a test post"
                        "userId" 1}
             response (-> (SerenityRest/given)
                          (.baseUri "https://jsonplaceholder.typicode.com")
                          (.contentType ContentType/JSON)
                          (.body post-data)
                          (.when)
                          (.post "/posts")
                          (.then)
                          (.statusCode 201)
                          (.extract)
                          (.response))]
         (is (= "Test Post" (-> response .jsonPath (.getString "title"))))
         (is (some? (-> response .jsonPath (.getInt "id"))))))))
