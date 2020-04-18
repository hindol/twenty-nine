(ns twenty-nine.backend.server
  (:require
   [com.stuartsierra.component :as c]
   [io.pedestal.http :as http]
   [io.pedestal.http.jetty.websockets :as ws]
   [io.pedestal.http.route :as route]))

(defn create-routes
  []
  (route/expand-routes #{}))

(def ws-paths
  {"/ws" {:on-connect #(prn %&)
          :on-text    #(prn %&)
          :on-binary  #(prn %&)
          :on-error   #(prn %&)
          :on-close   #(prn %&)}})

(defn create-service-map
  []
  {:env                    :prod
   ::http/routes            (create-routes)
   ::http/resource-path     "/public"
   ::http/type              :jetty
   ::http/container-options {:context-configurator #(ws/add-ws-endpoints % ws-paths)}
   ::http/secure-headers    {:content-security-policy-settings "object-src 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https: wss:;"}})

(defn create-server
  [service-map]
  (http/create-server service-map))

(defn start-server
  [server]
  (http/start server))

(defn stop-server
  [server]
  (http/stop server))

(defrecord WebServer [web-server service-map]
  c/Lifecycle
  (start [this]
    (let [server (create-server service-map)]
      (assoc this :web-server (start-server server))))

  (stop [this]
    (assoc this :web-server (stop-server web-server))))

(defn create-web-server
  [service-map]
  (map->WebServer {:service-map service-map}))
