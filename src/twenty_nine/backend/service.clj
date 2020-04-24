(ns twenty-nine.backend.service
  (:require
   [io.pedestal.http :as http]
   [io.pedestal.http.jetty.websockets :as ws]
   [io.pedestal.http.ring-middlewares :as ring]
   [twenty-nine.backend.ws]
   [twenty-nine.common.utils :as u]))

(def ^:private ws-paths
  {"/ws" {}})

(defmulti create-service-map :env)

(defmethod create-service-map :prod
  [{:keys [routes-fn ws-server access-tokens]}]
  (-> {:env                    :prod
       ::http/type              :jetty
       ::http/routes            (routes-fn)
       ::http/resource-path     "/public"
       ::http/mime-types        {nil "text/html"} ;; No extension in URL => assume HTML
       ::http/container-options {:context-configurator #(ws/add-ws-endpoints
                                                         %
                                                         ws-paths
                                                         {:listener-fn (twenty-nine.backend.ws/create-connection-listener
                                                                        {:access-tokens access-tokens
                                                                         :ws-clients    (:ws-clients ws-server)})})}
       ::http/secure-headers    {:content-security-policy-settings "object-src 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval' https: wss:;"}}
      http/default-interceptors
      (update ::http/interceptors (fn prefer-fast-resource [interceptors]
                                    (if-let [idx (u/position #(-> % :name (= ::ring/resource)) interceptors)]
                                      (assoc interceptors idx (ring/fast-resource "/dev"))
                                      interceptors)))))

(defmethod create-service-map :dev
  [{:keys [routes-fn]
    :as   opts}]
  (-> (merge (create-service-map (merge opts {:env :prod}))
             {:env                  :dev
              ::http/routes          routes-fn
              ::http/join?           false
              ::http/resource-path   "/dev"
              ::http/allowed-origins {:creds           true
                                      :allowed-origins (constantly true)}
              ::http/secure-headers  {:content-security-policy-settings "object-src 'none'; script-src 'self' 'unsafe-inline' 'unsafe-eval' http: ws:;"}})
      http/dev-interceptors))
