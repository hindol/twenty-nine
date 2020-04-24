(ns twenty-nine.backend.ws
  (:require
   [clojure.core.async :as a]
   [clojure.pprint :as pp]
   [com.stuartsierra.component :as c]
   [io.pedestal.http.jetty.websockets :as ws]
   [twenty-nine.backend.api :as api])
  (:import
   (org.eclipse.jetty.websocket.api Session
                                    WebSocketConnectionListener
                                    WebSocketListener)
   (org.eclipse.jetty.websocket.servlet ServletUpgradeRequest)))

(defrecord WebSocketClient [ws-clients ^Session session send-ch]
  WebSocketConnectionListener
  (onWebSocketConnect [this session]
    (assoc this :session session))
  (onWebSocketClose [this status-code reason]
    (.close session))
  (onWebSocketError [this cause])

  WebSocketListener
  (onWebSocketText [this text])
  (onWebSocketBinary [this payload offset length]))

(defn create-client
  [ws-clients]
  (ws/start-ws-connection
   (fn [session send-ch]
     (let [client (map->WebSocketClient {:session session
                                         :send-ch send-ch})]
       (swap! ws-clients assoc session client)
       client))))

(defn read-cookie
  [cookies name]
  (some #(when (-> % .getName (= name))
           (.getValue %))
        cookies))

(defn create-connection-listener
  [{:keys [access-tokens ws-clients]}]
  (fn connection-listener
    [^ServletUpgradeRequest request _ _]
    (let [token       (-> request .getCookies (read-cookie "access-token"))
          authorized? (api/authorized? access-tokens token
                                       {:remote-addr (.getRemoteAddress request)})]
      (when authorized?
       (map->WebSocketClient {:ws-clients ws-clients})))))

(defrecord WebSocketServer [ws-server access-tokens]
  c/Lifecycle
  (start [this]
    (assoc this :ws-server {:ws-clients (atom {})}))

  (stop [this]
    (let [ws-clients (:ws-clients ws-server)]
      (doseq [[_ send-ch] @ws-clients]
        (a/close! send-ch)))))

(defn create-websocket-server
  []
  (map->WebSocketServer {}))
