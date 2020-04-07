(ns dev.user
  (:require
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
                               ::http/allowed-origins {:creds           true
                                                       :allowed-origins (constantly true)}
                               ::http/host            "127.0.0.1"
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

(defn reset
  []
  (stop-dev)
  (start-dev))

(reset)