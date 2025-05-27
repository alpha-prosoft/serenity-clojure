(ns samples.demo-test
  (:require [clojure.test :refer [deftest is]]
            [testing.junit :refer [with-serenity step take-screenshot]]))

(deftest simple-ui-test
  (with-serenity [page]
    (step "Navigate to example.com"
      #(do
         (.navigate page "https://example.com")
         (Thread/sleep 2000)
         (take-screenshot page "example-homepage")))
    
    (step "Verify page title"
      #(do
         (let [title (.title page)]
           (is (clojure.string/includes? title "Example"))
           (take-screenshot page "title-verified"))))))
