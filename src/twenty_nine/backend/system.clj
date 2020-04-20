(ns twenty-nine.backend.system
  (:require
   [com.stuartsierra.component :as c]
   [twenty-nine.backend.server :as server]
   [twenty-nine.backend.service :as service]
   [twenty-nine.backend.ws :as ws]))

(defn create-system
  [{:keys [env host port]}]
  (c/system-map
   :service-map (service/create-service-map {:env env})
   :ws-server   (c/using
                 (ws/create-websocket-server)
                 [:service-map])
   :web-server  (c/using
                 (server/create-web-server {:host host
                                            :port port})
                 [:service-map :ws-server])))
