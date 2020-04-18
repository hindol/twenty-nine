(ns user
  (:require
   [clojure.java.classpath :as cp]
   [clojure.tools.namespace.repl :as repl]
   [com.stuartsierra.component :as c]
   [io.pedestal.http :as http]
   [twenty-nine.backend.server :as server]
   [twenty-nine.backend.system :as system]))

(defn create-dev-service-map
  []
  (merge (server/create-service-map)
         {:env                  :dev
          ::http/join?           false
          ::http/resource-path   "/dev"
          ::http/allowed-origins {:creds           true
                                  :allowed-origins (constantly true)}
          ::http/secure-headers  {:content-security-policy-settings "object-src 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval' http: ws:;"}
          ::http/host            "0.0.0.0"
          ::http/port            8080}))

(defonce system
  (atom nil))

(defn init
  []
  (reset! system (system/create-system {:service-map (create-dev-service-map)})))

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
  (cp/classpath)
  (binding [*compile-files* true]
    (require 'user :reload-all))
  (reset))
