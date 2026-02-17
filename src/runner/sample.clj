(ns runner.sample
  (:require [testing.repl :refer [page]]))

(.navigate @page "https://example.com")
(.textContent (.locator @page "h1"))
(.screenshot @page)
