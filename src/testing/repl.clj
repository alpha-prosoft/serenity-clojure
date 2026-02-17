(ns testing.repl
  "nREPL helper for interactive browser exploration.
   Starts an nREPL server and a headed Playwright browser.

   Usage:
     clj -M:nrepl

   Then connect your editor to the nREPL port printed on startup.

   Example:
     (require '[testing.repl :refer [page restart stop]])
     (.navigate @page \"https://example.com\")
     (.textContent (.locator @page \"h1\"))
     (.screenshot @page)"
  (:require [testing.junit :as junit]
            [nrepl.server :as nrepl]))

(defonce page (atom nil))
(defonce ^:private browser-map (atom nil))

;; ── Browser lifecycle ───────────────────────────────────────────────

(defn stop
  "Close the browser (if running)."
  []
  (when @browser-map
    (try
      (junit/stop-browser @browser-map)
      (catch Exception _))
    (reset! browser-map nil)
    (reset! page nil)
    (println "Browser stopped.")))

(defn restart
  "Close any existing browser and start a fresh headed Chromium instance.
   Returns the Page object."
  []
  (stop)
  (let [bm (junit/start-browser {:headless false})]
    (reset! browser-map bm)
    (reset! page (:page bm))
    (println "Browser started.")
    @page))

;; ── Startup ─────────────────────────────────────────────────────────

(defn- cider-handler
  "Build an nREPL handler with cider-nrepl middleware if available,
   otherwise fall back to the default handler."
  []
  (try
    (require 'cider.nrepl)
    (let [handler-fn (resolve 'cider.nrepl/cider-nrepl-handler)]
      (if handler-fn
        (do (println "  cider-nrepl middleware loaded.")
            @handler-fn)
        (nrepl.server/default-handler)))
    (catch Exception _
      (println "  cider-nrepl not found, using default handler.")
      (nrepl.server/default-handler))))

(defn -main
  "Entry point: start nREPL server, launch browser, print instructions."
  [& args]
  (let [port (if (seq args)
               (Integer/parseInt (first args))
               7888)
        handler (cider-handler)
        server (nrepl/start-server :port port :handler handler)]
    (spit ".nrepl-port" (str port))
    (println)
    (println "========================================")
    (println " Serenity Clojure - Interactive REPL")
    (println "========================================")
    (println (str "  nREPL server on port " port))
    (println "  Connect your editor to this port.")
    (println "  Starting headed browser...")
    (println)

    (restart)

    (println)
    (println "  Ready! Quick reference:")
    (println "    (require '[testing.repl :refer [page restart stop]])")
    (println)
    (println "    (.navigate @page \"https://example.com\")")
    (println "    (.textContent (.locator @page \"h1\"))")
    (println "    (.screenshot @page)")
    (println "    (restart)  - restart browser")
    (println "    (stop)     - close browser")
    (println "========================================")
    (println)

    @(promise)))
