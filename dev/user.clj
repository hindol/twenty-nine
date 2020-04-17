(ns dev.user
  (:require
   [clojure.tools.namespace.repl :as repl]
   [com.github.hindol.twenty-nine :as core]
   [io.pedestal.http :as http]))

(defonce server (atom nil))

(defn start-dev
  []
  (when-not @server
    (reset! server (-> core/service
                       (merge {:env                  :dev
                               ::http/join?           false
                               ::http/routes          #(deref #'core/routes)
                               ::http/resource-path   "/dev"
                               ::http/allowed-origins {:creds           true
                                                       :allowed-origins (constantly true)}
                               ::http/host            "0.0.0.0"
                               ::http/port            8080})
                       http/default-interceptors
                       http/dev-interceptors
                       http/create-server))
    (http/start @server)))

(defn stop-dev
  []
  (when @server
    (http/stop @server)
    (reset! server nil)))

(defn refresh
  []
  (repl/refresh))

(defn reset
  []
  (stop-dev)
  (start-dev))

(refresh)
