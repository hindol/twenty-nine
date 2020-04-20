(ns twenty-nine.backend.ws
  (:require
   [clojure.pprint :as pp]
   [com.stuartsierra.component :as c])
  (:import
   (org.eclipse.jetty.websocket.servlet ServletUpgradeRequest
                                        ServletUpgradeResponse)))

(defn connection-listener
  [^ServletUpgradeRequest request ^ServletUpgradeResponse response fn-map]
  (pp/pprint (bean request)))

(def fn-map
  {:on-connect #(prn %&)
   :on-text    #(prn %&)
   :on-binary  #(prn %&)
   :on-error   #(prn %&)
   :on-close   #(prn %&)})

(defrecord WebSocketServer [ws-server service-map]
  c/Lifecycle
  (start [this]
    (assoc this :ws-server {:ws-clients (atom {})
                            :fn-map     fn-map}))

  (stop [this]
    (let [ws-clients (:ws-clients ws-server)]
      (reset! ws-clients nil))))

(defn create-websocket-server
  []
  (map->WebSocketServer {}))
