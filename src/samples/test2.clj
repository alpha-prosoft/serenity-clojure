(ns samples.test2
  (:require [clojure.test :refer [deftest is]]))

(deftest specific-util-test
  (is (= "HELLO" (.toUpperCase "hello"))))

(deftest another-util-test
  (is (string? (str 1 2 3))))
