run: 
	rm -rf classes/* && clojure -M -e "(compile 'runner.ui)" && echo "####RUN####" && clj -M -m runner.ui && ls -la classes

