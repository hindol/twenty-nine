(ns twenty-nine.backend.server
  (:require
   [com.stuartsierra.component :as c]
   [io.pedestal.http :as http]
   [twenty-nine.backend.ws]))

(defn ^:private create-server
  [service-map]
  (http/create-server service-map))

(defn ^:private start-server
  [server]
  (http/start server))

(defn ^:private stop-server
  [server]
  (http/stop server))

(defrecord WebServer [web-server service-map host port]
  c/Lifecycle
  (start [this]
    (let [server (create-server (-> service-map
                                    (merge {::http/host host
                                            ::http/port port})))]
      (assoc this :web-server (start-server server))))

  (stop [this]
    (assoc this :web-server (stop-server web-server))))

(defn create-web-server
  [{:keys [host port]}]
  (map->WebServer {:host host
                   :port port}))
