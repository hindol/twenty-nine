(ns com.github.hindol.twenty-nine
  (:require
   [clojure.core.async :as async]
   [io.pedestal.http :as http]
   [io.pedestal.http.jetty.websockets :as ws]
   [io.pedestal.http.route :as route]
   [io.pedestal.test :as test]))

(defonce ws-clients (atom {}))

(defn ws-client
  [ws-session send-ch]
  (async/put! send-ch (str "(ws-client " ws-session " " send-ch ")"))
  (swap! ws-clients assoc ws-session send-ch))

(defn send-message-to-all!
  [message]
  (doseq [[_session channel] @ws-clients]
    (async/put! channel message)))

(def ws-paths
  {"/ws" {:on-connect (ws/start-ws-connection ws-client)
          :on-text    (fn [message] (prn "A client sent - " message))
          :on-binary  (fn [payload _offset _length] (prn "A client sent - " payload))
          :on-error   (fn [e] (prn "Socket error - " e))
          :on-close   (fn [_code reason] (prn "Socket closed - " reason))}})

(def routes
  (route/expand-routes
   #{}))

(def service
  {:env                    :prod
   ::http/routes            routes
   ::http/type              :jetty
   ::http/container-options {:context-configurator #(ws/add-ws-endpoints % ws-paths)}
   ::http/port              8080})

(defonce server (atom nil))

(defn start-dev
  []
  (reset! server (-> service
                     (merge {:env                  :dev
                             ::http/join?           false
                             ::http/routes          #(deref #'routes)
                             ::http/allowed-origins {:creds           true
                                                     :allowed-origins (constantly true)}
                             ::http/host            "0.0.0.0"})
                     http/default-interceptors
                     http/dev-interceptors
                     http/create-server))
  (http/start @server))

(defn stop-dev
  []
  (http/stop @server)
  (reset! server nil))

(defn -main
  [& _]
  (start-dev)
  (stop-dev))
