(ns samples.google-api-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.string :as str]
            [testing.junit :refer [with-serenity step api-step take-screenshot]])
  (:import [net.serenitybdd.rest SerenityRest]
           [io.restassured.http ContentType]
           [com.microsoft.playwright.options LoadState]))

(deftest comprehensive-test
  (with-serenity [page]
    
    ;; UI Testing - Example.com
    (step "Navigate to example.com"
      #(do
         (.navigate page "https://example.com")
         (.waitForLoadState page LoadState/NETWORKIDLE)
         (take-screenshot page "example-homepage")))
    
    (step "Verify page heading"
      #(let [heading (.locator page "h1")]
         (is (> (.count heading) 0) "Page should have an h1 heading")
         (is (str/includes? (.textContent (.first heading)) "Example Domain")
             "Heading should say 'Example Domain'")
         (take-screenshot page "heading-verified")))
    
    (step "Verify page has descriptive content"
      #(let [paragraphs (.locator page "p")]
         (is (> (.count paragraphs) 0) "Page should have paragraphs")
         (take-screenshot page "content-verified")))
    
    ;; API Testing - JSONPlaceholder
     (api-step "Fetch user from JSONPlaceholder"
       #(let [response (-> (SerenityRest/given)
                           (.baseUri "https://jsonplaceholder.typicode.com")
                           (.when)
                           (.get "/users/1" (into-array Object [])))]
           (is (= 200 (.statusCode response)))
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
                           (.post "/posts" (into-array Object [])))]
           (is (= 201 (.statusCode response)))
           (is (= "Test Post" (-> response .jsonPath (.getString "title"))))
           (is (some? (-> response .jsonPath (.getInt "id"))))))))
