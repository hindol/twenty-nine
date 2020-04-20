(ns user
  (:require
   [clojure.tools.namespace.repl :as repl]
   [com.stuartsierra.component :as c]
   [twenty-nine.backend.system :as system]))

(defonce system
  (atom nil))

(defn init
  []
  (reset! system (system/create-system {:env  :dev
                                        :host "0.0.0.0"
                                        :port 8080})))

(defn start
  []
  (swap! system c/start))

(defn stop
  []
  (when @system
    (swap! system c/stop)))

(defn go
  []
  (init)
  (start))

(defn reset
  []
  (stop)
  (repl/refresh :after 'user/go))

(comment
  (reset))
