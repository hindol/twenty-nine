(ns com.github.hindol.twenty-nine
  (:require
   [clojure.core.async :as async]
   [clojure.pprint :as pp]
   [io.pedestal.http :as http]
   [io.pedestal.http.jetty.websockets :as ws]
   [io.pedestal.http.route :as route]
   [io.pedestal.test :as test]))

(defonce ws-clients (atom {}))

(defn ws-client
  [ws-session send-ch]
  (async/put! send-ch (with-out-str (pp/pprint ws-session)))
  (swap! ws-clients assoc ws-session send-ch))

(defn send-message-to-all!
  [message]
  (doseq [[_session channel] @ws-clients]
    (async/put! channel message)))

(def ws-paths
  {"/ws" {:on-connect (ws/start-ws-connection ws-client)
          :on-text    (fn [message] (println "A client sent - " message))
          :on-binary  (fn [payload _offset _length] (println "A client sent - " payload))
          :on-error   (fn [e] (println "Socket error - " e))
          :on-close   (fn [code reason]
                        (println "Socket closed - " code reason))}})

(def routes
  (route/expand-routes
   #{}))

(def service
  {:env                    :prod
   ::http/routes            routes
   ::http/type              :jetty
   ::http/container-options {:context-configurator #(ws/add-ws-endpoints % ws-paths)}})

(defonce server (atom nil))

(defn start-dev
  []
  (when-not @server
    (reset! server (-> service
                       (merge {:env                  :dev
                               ::http/join?           false
                               ::http/routes          #(deref #'routes)
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

(defn -main
  [& {:as args}]
  (-> service
      (merge {::http/host (get args "host")
              ::http/port (Integer/parseInt (get args "port"))})
      http/create-server
      http/start))
