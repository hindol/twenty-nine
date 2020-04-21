(ns twenty-nine.backend.system
  (:require
   [com.stuartsierra.component :as c]
   [twenty-nine.backend.api :as api]
   [twenty-nine.backend.server :as server]
   [twenty-nine.backend.service :as service]
   [twenty-nine.backend.ws :as ws]))

(defn create-system
  [{:keys [env host port]}]
  (let [tokens    (atom {})
        routes-fn #(api/create-routes {:access-tokens tokens})]
    (c/system-map
     :access-tokens tokens
     :routes-fn     routes-fn
     :service-map   (service/create-service-map {:env           env
                                                 :routes-fn     routes-fn})
     :ws-server     (c/using
                     (ws/create-websocket-server)
                     [:service-map :access-tokens])
     :web-server    (c/using
                     (server/create-web-server {:host host
                                                :port port})
                     [:service-map :ws-server]))))
