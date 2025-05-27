(ns samples.test1
  (:require [clojure.test :refer [deftest is testing]]
            [runner.gen :refer [*test-info*]]
            [etaoin.api :as e]))


(deftest addition-test
  (let [driver (e/chrome)]
    (try
      (e/go driver "https://dev01-samurai.web-samurai.localdevhub.com:3003")
      (println "ABBBBBBBBBBBBBBBBBBBBBBBB" (.getDisplayName *test-info*))
      (testing "Basic addition"
        (is (= 4 (+ 2 2)))
        (is (= 0 (+ 0 0))))
      (finally
        (e/quit driver)))))

(deftest failing-test
  (testing "A test that fails"
    (is (= 5 (+ 2 2)))))

