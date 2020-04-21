(ns twenty-nine.backend.ws
  (:require
   [clojure.core.async :as a]
   [clojure.pprint :as pp]
   [com.stuartsierra.component :as c]
   [io.pedestal.http.jetty.websockets :as ws])
  (:import
   (org.eclipse.jetty.websocket.api Session
                                    WebSocketConnectionListener
                                    WebSocketListener)))

(defn fn-map
  [{:keys [ws-clients]}]
  {:on-connect (ws/start-ws-connection
                (fn [session send-ch]
                  (swap! ws-clients assoc session send-ch)))
   :on-text    (fn [text])
   :on-binary  #(pp/pprint %&)
   :on-error   #(pp/pprint %&)
   :on-close   #(pp/pprint %&)})

(defrecord WebSocketServer [ws-server service-map access-tokens]
  c/Lifecycle
  (start [this]
    (assoc this :ws-server {:ws-clients (atom {})
                            :fn-map     fn-map}))

  (stop [this]
    (let [ws-clients (:ws-clients ws-server)]
      (doseq [[_ send-ch] @ws-clients]
        (a/close! send-ch)))))

(defn create-websocket-server
  []
  (map->WebSocketServer {}))
