(ns com.github.hindol.twenty-nine
  (:require
   [clojure.core.async :as async]
   [io.pedestal.http :as http]
   [io.pedestal.http.jetty.websockets :as ws]
   [io.pedestal.http.route :as route])
  (:import
   (org.eclipse.jetty.websocket.api Session
                                    WebSocketAdapter)))

(def ws-clients (atom {}))

(defn add-client
  [ws-session send-ch]
  (async/put! send-ch (pr-str ws-session))
  (swap! ws-clients assoc ws-session send-ch))

(defn ws-listener
  [_request _response ws-map]
  (proxy [WebSocketAdapter] []
    (onWebSocketConnect [^Session ws-session]
      (proxy-super onWebSocketConnect ws-session)
      (when-let [f (:on-connect ws-map)]
        (f ws-session)))
    (onWebSocketClose [status-code reason]
      (when-let [f (:on-close ws-map)]
        (f (.getSession this) status-code reason)))
    (onWebSocketError [^Throwable e]
      (when-let [f (:on-error ws-map)]
        (f (.getSession this) e)))

    (onWebSocketText [^String message]
      (when-let [f (:on-text ws-map)]
        (f (.getSession this) message)))
    (onWebSocketBinary [^bytes payload offset length]
      (when-let [f (:on-binary ws-map)]
        (f (.getSession this) payload offset length)))))

(defn broadcast!
  ([message] (broadcast! @ws-clients message))
  ([ws-clients message]
   (doseq [[^Session ws-session channel] ws-clients]
     (when (.isOpen ws-session)
       (async/put! channel message)))))

(count @ws-clients)

(def ws-paths
  {"/ws" {:on-connect (ws/start-ws-connection add-client)
          :on-text    (fn on-text
                        [_ws-session message]
                        (println "A client sent - " message))
          :on-binary  (fn on-binary
                        [_ws-session payload _offset _length]
                        (println "A client sent - " payload))
          :on-error   (fn on-error
                        [_ws-session e]
                        (println "Socket error - " e))
          :on-close   (fn on-close
                        [ws-session _code _reason]
                        (swap! ws-clients dissoc ws-session))}})

(def routes
  (route/expand-routes
   #{}))

(def service
  {:env                    :prod
   ::http/routes            routes
   ::http/type              :jetty
   ::http/container-options {:context-configurator #(ws/add-ws-endpoints % ws-paths {:listener-fn ws-listener})}})

(defn -main
  [& {:as args}]
  (-> service
      (merge {::http/host (get args "host")
              ::http/port (Integer/parseInt (get args "port"))})
      http/create-server
      http/start))
